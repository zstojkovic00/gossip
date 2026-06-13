// +build ignore

/*
 * Copyright 2018- The Pixie Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */


 // Kod je adaptiran tako da radi sa cilium/ebpf go

#include "vmlinux.h"
#include <bpf/bpf_helpers.h>
#include <bpf/bpf_core_read.h>
#include <bpf/bpf_endian.h>

#define socklen_t size_t

// Data buffer message size. BPF can submit at most this amount of data to a perf buffer.
// Kernel size limit is 32KiB. See https://github.com/iovisor/bcc/issues/2519 for more details.
#define MAX_MSG_SIZE 30720 // 30KiB


// This defines how many chunks a perf_submit can support.
// This applies to messages that are over MAX_MSG_SIZE,
// and effectively makes the maximum message size to be CHUNK_LIMIT*MAX_MSG_SIZE.
// #define CHUNK_LIMIT 4

enum traffic_direction_t {
    kEgress,
    kIngress,
};

// Structs

// A struct representing a unique ID that is composed of the pid, the file
// descriptor and the creation time of the struct.
struct conn_id_t {
    // Process ID
    u32 pid;
    // The file descriptor to the opened network connection.
    u32 fd;
    // Timestamp at the initialization of the struct.
    u64 tsid;
};


// This struct contains information collected when a connection is established,
// via an accept4() syscall.
struct conn_info_t {
    // Connection identifier.
    struct conn_id_t conn_id;

    // The number of bytes written/read on this connection.
    u64 wr_bytes;
    u64 rd_bytes;

    // A flag indicating we identified the connection as HTTP.
    bool is_http;

    // Kernel socket pointer
    u64 skaddr;

    // Connection tuple (from client perspective, matching TCP events)
    u32 saddr;
    u32 daddr;
    u16 sport;
    u16 dport;
};

// An helper struct that hold the addr argument of the syscall.
struct accept_args_t {
    u64 addr;
};


// An helper struct to cache input argument of read/write syscalls between the
// entry hook and the exit hook.
struct data_args_t {
    u32 fd;
    u64 buf;
};


struct close_args_t {
    u32 fd;
};

struct socket_data_event_t {
  // We split attributes into a separate struct, because BPF gets upset if you do lots of
  // size arithmetic. This makes it so that it's attributes followed by message.
  struct attr_t {
    // The timestamp when syscall completed (return probe was triggered).
    u64 timestamp_ns;

    // Connection identifier (PID, FD, etc.).
    struct conn_id_t conn_id;

    // The type of the actual data that the msg field encodes, which is used by the caller
    // to determine how to interpret the data.
    enum traffic_direction_t direction;

	// The size of the original message. We use this to truncate msg field to minimize the amount
    // of data being transferred.
    u64 msg_size;

    // A 0-based position number for this event on the connection, in terms of byte position.
    // The position is for the first byte of this message.
    u64 pos;

    // Kernel socket pointer
    u64 skaddr;

    // Connection tuple (from client perspective, matching TCP events)
    u32 saddr;
    u32 daddr;
    u16 sport;
    u16 dport;
    u32 _pad2;
  } attr;
  char msg[MAX_MSG_SIZE];
};


// Maps


// A map of the active connections. The name of the map is conn_info_map
// the key is of type uint64_t, the value is of type struct conn_info_t,
// and the map won't be bigger than 128KB.
struct {
      __uint(type, BPF_MAP_TYPE_HASH);
      __uint(max_entries, 131072);
      __type(key, u64);
      __type(value, struct conn_info_t);
} conn_info_map SEC(".maps");

// An helper map that will help us cache the input arguments of the accept syscall
// between the entry hook and the return hook.
struct {
    __uint(type, BPF_MAP_TYPE_HASH);
    __uint(max_entries, 10240);
    __type(key, u64);
    __type(value, struct accept_args_t);
} active_accept_args_map SEC(".maps");

// Ring buffer to send to the user-mode the data events.
struct {
    __uint(type, BPF_MAP_TYPE_RINGBUF);
    __uint(max_entries, 1 << 24);
} socket_data_events SEC(".maps");

struct {
    __uint(type, BPF_MAP_TYPE_HASH);
    __uint(max_entries, 10240);
    __type(key, u64);
    __type(value, struct data_args_t);
} active_write_args_map SEC(".maps");

// Helper map to store read syscall arguments between entry and exit hooks.
struct {
    __uint(type, BPF_MAP_TYPE_HASH);
    __uint(max_entries, 10240);
    __type(key, u64);
    __type(value, struct data_args_t);
} active_read_args_map SEC(".maps");

// An helper map to store close syscall arguments between entry and exit syscalls.
struct {
    __uint(type, BPF_MAP_TYPE_HASH);
    __uint(max_entries, 10240);
    __type(key, u64);
    __type(value, struct close_args_t);
} active_close_args_map SEC(".maps");

// Helper functions

// Generates a unique identifier using a tgid (Thread Global ID) and a fd (File Descriptor).
static __inline u64 gen_tgid_fd(u32 tgid, int fd) {
    return ((u64)tgid << 32) | (u32)fd;
}

// Navigates task_struct → files → fdt → fd[fd] → socket → sk,
static __inline void fill_sk_fields(struct conn_info_t *conn_info, s32 fd) {
    struct task_struct *task = (struct task_struct *)bpf_get_current_task();

    struct files_struct *files;
    if (bpf_core_read(&files, sizeof(files), &task->files) != 0) return;

    struct fdtable *fdt;
    if (bpf_core_read(&fdt, sizeof(fdt), &files->fdt) != 0) return;

    struct file **fd_arr;
    if (bpf_core_read(&fd_arr, sizeof(fd_arr), &fdt->fd) != 0) return;

    struct file *file;
    if (bpf_core_read(&file, sizeof(file), &fd_arr[fd]) != 0) return;

    struct socket *sock;
    if (bpf_core_read(&sock, sizeof(sock), &file->private_data) != 0) return;

    struct sock *sk;
    if (bpf_core_read(&sk, sizeof(sk), &sock->sk) != 0) return;

    conn_info->skaddr = (u64)sk;
    conn_info->saddr  = BPF_CORE_READ(sk, __sk_common.skc_daddr);
    conn_info->daddr  = BPF_CORE_READ(sk, __sk_common.skc_rcv_saddr);
    conn_info->sport  = bpf_ntohs(BPF_CORE_READ(sk, __sk_common.skc_dport));
    conn_info->dport  = BPF_CORE_READ(sk, __sk_common.skc_num);
}

// Checks the first bytes of the buffer to determine if the data looks like HTTP.
static __inline bool is_http(const char *buf, size_t count) {
    if (count < 4) return false;
    char b[4];
    if (bpf_probe_read_user(b, sizeof(b), buf) != 0) return false;
    return (b[0] == 'G' && b[1] == 'E' && b[2] == 'T') ||  // GET
           (b[0] == 'P' && b[1] == 'O' && b[2] == 'S') ||  // POST
           (b[0] == 'H' && b[1] == 'T' && b[2] == 'T') ||  // HTTP (response)
           (b[0] == 'P' && b[1] == 'U' && b[2] == 'T') ||  // PUT
           (b[0] == 'D' && b[1] == 'E' && b[2] == 'L') ||  // DELETE
           (b[0] == 'H' && b[1] == 'E' && b[2] == 'A') ||  // HEAD
           (b[0] == 'P' && b[1] == 'A' && b[2] == 'T') ||  // PATCH
           (b[0] == 'C' && b[1] == 'O' && b[2] == 'N');    // CONNECT/OPTIONS
}

// An helper function that checks if the syscall finished successfully and if it did
// saves the new connection in a dedicated map of connections
static __inline void process_syscall_accept(u64 id,
                                            s64 ret_fd,
                                            const struct accept_args_t *args) {
    // Extracting the return code, and checking if it represent a failure,
    // if it does, we abort the as we have nothing to do.
    if (ret_fd <= 0) {
        return;
    }

    u32 pid = id >> 32;
    u64 tgid_fd = gen_tgid_fd(pid, (s32)ret_fd);

    struct conn_info_t conn_info = {};
    conn_info.conn_id.pid = pid;
    conn_info.conn_id.fd = (s32)ret_fd;
    conn_info.conn_id.tsid = bpf_ktime_get_ns();
    fill_sk_fields(&conn_info, (s32)ret_fd);

    // Saving the connection info in a global map, so in the other syscalls
    // (read, write and close) we will be able to know that we have seen
    // the connection
    bpf_map_update_elem(&conn_info_map, &tgid_fd, &conn_info, BPF_ANY);
}

// Processes read/write syscall data. Only submits events for connections we identified as HTTP.
static __inline void process_syscall_data(u64 id,
                                          s64 bytes_count,
                                          const struct data_args_t *args,
                                          enum traffic_direction_t direction) {
    if (bytes_count <= 0) {
        return;
    }

    u32 tgid = id >> 32;
    u64 tgid_fd = gen_tgid_fd(tgid, args->fd);

    struct conn_info_t *conn_info = bpf_map_lookup_elem(&conn_info_map, &tgid_fd);
    if (!conn_info) {
        // The FD does not represent a connection we are tracking.
        return;
    }

    // On the first data event, check if this connection is HTTP.
    // If it's not, we ignore all future events on this connection.
    if (!conn_info->is_http) {
        if (!is_http((const char *)args->buf, (size_t)bytes_count)) return;
        conn_info->is_http = true;
    }

    // Track byte position for this message on the connection.
    u64 pos;
    if (direction == kEgress) {
        pos = conn_info->wr_bytes;
        conn_info->wr_bytes += bytes_count;
    } else {
        pos = conn_info->rd_bytes;
        conn_info->rd_bytes += bytes_count;
    }

    struct socket_data_event_t *event = bpf_ringbuf_reserve(&socket_data_events, sizeof(*event), 0);
    if (!event) {
        bpf_printk("bpf_ringbuf_reserve on socket_data_event failed\n");
        return;
    }

    event->attr.timestamp_ns = bpf_ktime_get_ns();
    event->attr.conn_id = conn_info->conn_id;
    event->attr.direction = direction;
    event->attr.msg_size = bytes_count;
    event->attr.pos = pos;
    event->attr.skaddr = conn_info->skaddr;
    event->attr.saddr  = conn_info->saddr;
    event->attr.daddr  = conn_info->daddr;
    event->attr.sport  = conn_info->sport;
    event->attr.dport  = conn_info->dport;
    bpf_probe_read_user(event->msg, MAX_MSG_SIZE, (void *)args->buf);
    bpf_ringbuf_submit(event, 0);
}

static inline __attribute__((__always_inline__)) void process_syscall_close(u64 id,
                                                                            s64 ret_fd,
                                                                            const struct close_args_t* close_args) {
    if (ret_fd < 0) {
        return;
    }

    u32 tgid = id >> 32;
    u64 tgid_fd = gen_tgid_fd(tgid, close_args->fd);

    // Remove the connection from the mapping.
    bpf_map_delete_elem(&conn_info_map, &tgid_fd);
}

// Probe programs
SEC("tracepoint/syscalls/sys_enter_accept4")
int sys_enter_accept4(struct trace_event_raw_sys_enter *ctx) {
    u64 id = bpf_get_current_pid_tgid();
    struct accept_args_t args = {};
    args.addr = (u64)ctx->args[1];
    bpf_map_update_elem(&active_accept_args_map, &id, &args, BPF_ANY);
    return 0;
}

SEC("tracepoint/syscalls/sys_exit_accept4")
int sys_exit_accept4(struct trace_event_raw_sys_exit *ctx) {
    u64 id = bpf_get_current_pid_tgid();
    struct accept_args_t *args = bpf_map_lookup_elem(&active_accept_args_map, &id);
    if (args) {
        process_syscall_accept(id, ctx->ret, args);
        bpf_map_delete_elem(&active_accept_args_map, &id);
    }
    return 0;
}

SEC("tracepoint/syscalls/sys_enter_accept")
int sys_enter_accept(struct trace_event_raw_sys_enter *ctx) {
    u64 id = bpf_get_current_pid_tgid();
    struct accept_args_t args = {};
    args.addr = (u64)ctx->args[1];
    bpf_map_update_elem(&active_accept_args_map, &id, &args, BPF_ANY);
    return 0;
}

SEC("tracepoint/syscalls/sys_exit_accept")
int sys_exit_accept(struct trace_event_raw_sys_exit *ctx) {
    u64 id = bpf_get_current_pid_tgid();
    struct accept_args_t *args = bpf_map_lookup_elem(&active_accept_args_map, &id);
    if (args) {
        process_syscall_accept(id, ctx->ret, args);
        bpf_map_delete_elem(&active_accept_args_map, &id);
    }
    return 0;
}

SEC("tracepoint/syscalls/sys_enter_write")
int sys_enter_write(struct trace_event_raw_sys_enter *ctx) {
    u64 id = bpf_get_current_pid_tgid();
    struct data_args_t args = {};
    args.fd = (u32)ctx->args[0];
    args.buf = (u64)ctx->args[1];
    bpf_map_update_elem(&active_write_args_map, &id, &args, BPF_ANY);
    return 0;
}

SEC("tracepoint/syscalls/sys_exit_write")
int sys_exit_write(struct trace_event_raw_sys_exit *ctx) {
    u64 id = bpf_get_current_pid_tgid();
    struct data_args_t *args = bpf_map_lookup_elem(&active_write_args_map, &id);
    if (args) {
        process_syscall_data(id, ctx->ret, args, kEgress);
        bpf_map_delete_elem(&active_write_args_map, &id);
    }
    return 0;
}

SEC("tracepoint/syscalls/sys_enter_read")
int sys_enter_read(struct trace_event_raw_sys_enter *ctx) {
    u64 id = bpf_get_current_pid_tgid();
    struct data_args_t args = {};
    args.fd = (u32)ctx->args[0];
    args.buf = (u64)ctx->args[1];
    bpf_map_update_elem(&active_read_args_map, &id, &args, BPF_ANY);
    return 0;
}

SEC("tracepoint/syscalls/sys_exit_read")
int sys_exit_read(struct trace_event_raw_sys_exit *ctx) {
    u64 id = bpf_get_current_pid_tgid();
    struct data_args_t *args = bpf_map_lookup_elem(&active_read_args_map, &id);
    if (args) {
        process_syscall_data(id, ctx->ret, args, kIngress);
        bpf_map_delete_elem(&active_read_args_map, &id);
    }
    return 0;
}

SEC("tracepoint/syscalls/sys_enter_close")
int sys_enter_close(struct trace_event_raw_sys_enter *ctx) {
    u64 id = bpf_get_current_pid_tgid();
    struct close_args_t args = {};
    args.fd = (u32)ctx->args[0];
    bpf_map_update_elem(&active_close_args_map, &id, &args, BPF_ANY);
    return 0;
}

SEC("tracepoint/syscalls/sys_exit_close")
int sys_exit_close(struct trace_event_raw_sys_exit *ctx) {
    u64 id = bpf_get_current_pid_tgid();
    struct close_args_t *args = bpf_map_lookup_elem(&active_close_args_map, &id);
    if (args) {
        process_syscall_close(id, ctx->ret, args);
        bpf_map_delete_elem(&active_close_args_map, &id);
    }
    return 0;
}

char LICENSE[] SEC("license") = "GPL";
