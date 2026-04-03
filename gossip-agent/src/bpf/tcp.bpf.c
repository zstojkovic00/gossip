
// BTF je mapa koja pokazuje kako izgledaju sve kernel strukture
// /sys/kernel/btf/vmlinux je fajl koji kernel eksportuje i sadrzi sve te BTF informacije u binarnom formatu
#include "vmlinux.h" // bpftool btf dump file /sys/kernel/btf/vmlinux format c > src/bpf/vmlinux.h 

#include <bpf/bpf_helpers.h> // https://www.man7.org/linux/man-pages/man7/bpf-helpers.7.html


#define AF_INET     2   // IPv4
#define IPPROTO_TCP 6   // TCP protokol

struct tcp_event_t  {
    u32 pid;
    u32 saddr; // source ip (IPv4)
    u32 daddr; // destination IP (IPv4)
    u32 state; // TCP State
    u16 sport; // source port
    u16 dport; // destination port
    u8 comm[16]; // process name
};


// unix pipe, kernel -> golang
struct {        
    __uint(type, BPF_MAP_TYPE_RINGBUF);                                                                                                               
    __uint(max_entries, 1 << 24);
} events SEC(".maps"); 



SEC("tracepoint/sock/inet_sock_set_state")
int consume_tcp_event(struct trace_event_raw_inet_sock_set_state *args) // cat /sys/kernel/debug/tracing/events/sock/inet_sock_set_state/format
{
    if (args->protocol != IPPROTO_TCP) return 0;
    if (args->family != AF_INET) return 0;

    struct tcp_event_t *e = bpf_ringbuf_reserve(&events, sizeof(*e), 0);

    if (!e) {
        bpf_printk("bpf_ringbuf_reserve failed\n");
        return 1;
    }

    e->pid   = bpf_get_current_pid_tgid() >> 32;
    e->saddr = args->saddr[0];
    e->daddr = args->daddr[0];
    e->sport = args->sport;
    e->dport = args->dport;
    e->state = args->newstate;
    bpf_get_current_comm(&e->comm, sizeof(e->comm));

    bpf_ringbuf_submit(e, 0);

    return 0;
}

char LICENSE[] SEC("license") = "GPL";