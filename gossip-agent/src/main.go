package main

//go:generate go run github.com/cilium/ebpf/cmd/bpf2go -target bpfel -cc clang gen_tcp ./bpf/tcp.bpf.c -- -I/usr/include/bpf -I.

import (
	"bytes"
	"encoding/binary"
	"fmt"
	"log"
	"net"

	"github.com/cilium/ebpf/link"
	"github.com/cilium/ebpf/ringbuf"
	"golang.org/x/sys/unix"
)

const (
	TCP_ESTABLISHED  = 1
	TCP_SYN_SENT     = 2
	TCP_SYN_RECV     = 3
	TCP_FIN_WAIT1    = 4
	TCP_FIN_WAIT2    = 5
	TCP_TIME_WAIT    = 6
	TCP_CLOSE        = 7
	TCP_CLOSE_WAIT   = 8
	TCP_LAST_ACK     = 9
	TCP_LISTEN       = 10
	TCP_CLOSING      = 11
	TCP_NEW_SYN_RECV = 12
)

type tcp_event_t struct {
	Pid   uint32
	Saddr uint32
	Daddr uint32
	State uint32
	Sport uint16
	Dport uint16
	Comm  [16]byte
}

func setlimit() {
	if err := unix.Setrlimit(unix.RLIMIT_MEMLOCK,
		&unix.Rlimit{
			Cur: unix.RLIM_INFINITY,
			Max: unix.RLIM_INFINITY,
		}); err != nil {
		log.Fatalf("failed to set temporary rlimit: %v", err)
	}
}

func main() {
	setlimit()

	objs := gen_tcpObjects{}

	loadGen_tcpObjects(&objs, nil)
	link.Tracepoint("sock", "inet_sock_set_state", objs.ConsumeTcpEvent, nil)

	rd, err := ringbuf.NewReader(objs.Events)
	if err != nil {
		log.Fatalf("reader err")
	}

	for {
		ev, err := rd.Read()
		if err != nil {
			log.Fatalf("Read fail")
		}

		b_arr := bytes.NewBuffer(ev.RawSample)

		var data tcp_event_t
		if err := binary.Read(b_arr, binary.LittleEndian, &data); err != nil {
			log.Printf("parsing perf event: %s", err)
			continue
		}

		fmt.Printf("pid=%-6d comm=%-16s %s:%-5d → %s:%-5d state=%s\n",
			data.Pid,
			data.Comm,
			toIPv4(data.Saddr), data.Sport,
			toIPv4(data.Daddr), data.Dport,
			toTcpState(data.State))
	}
}

func toIPv4(ip uint32) net.IP {
	addr := make(net.IP, 4)
	binary.LittleEndian.PutUint32(addr, ip)
	return addr
}

func toTcpState(state uint32) string {
	switch state {
	case TCP_ESTABLISHED:
		return "ESTABLISHED"
	case TCP_SYN_SENT:
		return "SYN_SENT"
	case TCP_SYN_RECV:
		return "SYN_RECV"
	case TCP_FIN_WAIT1:
		return "FIN_WAIT1"
	case TCP_FIN_WAIT2:
		return "FIN_WAIT2"
	case TCP_TIME_WAIT:
		return "TIME_WAIT"
	case TCP_CLOSE:
		return "CLOSE"
	case TCP_CLOSE_WAIT:
		return "CLOSE_WAIT"
	case TCP_LAST_ACK:
		return "LAST_ACK"
	case TCP_LISTEN:
		return "LISTEN"
	case TCP_CLOSING:
		return "CLOSING"
	case TCP_NEW_SYN_RECV:
		return "NEW_SYN_RECV"
	default:
		return "UNKNOWN"
	}
}
