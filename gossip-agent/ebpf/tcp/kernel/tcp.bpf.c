// BTF describes the layout of all kernel structures
// /sys/kernel/btf/vmlinux is exported by the kernel and contains this BTF information in binary form
#include "vmlinux.h" // bpftool btf dump file /sys/kernel/btf/vmlinux format c > src/bpf/vmlinux.h 

#include <bpf/bpf_helpers.h> // https://www.man7.org/linux/man-pages/man7/bpf-helpers.7.html


#define AF_INET     2   // IPv4
#define AF_INET6    10  // IPv6
#define IPPROTO_TCP 6   // TCP protocol

struct tcp_event_t  {
    u64 timestamp_ns; // nanoseconds since boot, including suspend
    u32 pid;
    u32 saddr; // source IP (IPv4)
    u32 daddr; // destination IP (IPv4)
    u32 oldstate; // state before the transition
    u32 newstate; // state after the transition
    u16 sport; // source port
    u16 dport; // destination port
    u8 comm[16]; // process name
};

// Channel for sending events from the kernel to the Go program (ring buffer)
struct {        
    __uint(type, BPF_MAP_TYPE_RINGBUF);                                                                                                               
    __uint(max_entries, 1 << 24);
} events SEC(".maps"); 

// IPv4 address wrapped in IPv6 (::ffff:a.b.c.d), used by dual-stack sockets
static __always_inline bool is_v4_mapped(const __u8 *addr)
{
    #pragma unroll
    for (int i = 0; i < 10; i++) {
        if (addr[i] != 0) return false;
    }
    return addr[10] == 0xff && addr[11] == 0xff;
}

// eBPF
// bpf()
SEC("tracepoint/sock/inet_sock_set_state")
int consume_tcp_event(struct trace_event_raw_inet_sock_set_state *args) // cat /sys/kernel/debug/tracing/events/sock/inet_sock_set_state/format
{
    if (args->protocol != IPPROTO_TCP) return 0;

    u32 saddr;
    u32 daddr;

    // IPv4
    if (args->family == AF_INET) {
        bpf_probe_read_kernel(&saddr, sizeof(saddr), args->saddr);
        bpf_probe_read_kernel(&daddr, sizeof(daddr), args->daddr);
    }
    // IPv4 addresses wrapped in IPv6 format (::ffff:)
    else if (args->family == AF_INET6) {
        __u8 saddr_v6[16];
        __u8 daddr_v6[16];
        bpf_probe_read_kernel(saddr_v6, sizeof(saddr_v6), args->saddr_v6);
        bpf_probe_read_kernel(daddr_v6, sizeof(daddr_v6), args->daddr_v6);
        if (!is_v4_mapped(saddr_v6) || !is_v4_mapped(daddr_v6)) return 0;
        __builtin_memcpy(&saddr, &saddr_v6[12], 4);
        __builtin_memcpy(&daddr, &daddr_v6[12], 4);
    } else {
        return 0;
    }

    struct tcp_event_t *e = bpf_ringbuf_reserve(&events, sizeof(*e), 0);

    if (!e) {
        bpf_printk("bpf_ringbuf_reserve failed\n");
        return 1;
    }

    e->timestamp_ns = bpf_ktime_get_boot_ns();
    e->pid   = bpf_get_current_pid_tgid() >> 32;
    e->saddr = saddr;
    e->daddr = daddr;
    e->sport = args->sport;
    e->dport = args->dport;
    e->oldstate = args->oldstate;
    e->newstate = args->newstate;
    bpf_get_current_comm(&e->comm, sizeof(e->comm));

    bpf_ringbuf_submit(e, 0);
    return 0;
}

char LICENSE[] SEC("license") = "GPL";