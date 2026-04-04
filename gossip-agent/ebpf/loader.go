package ebpf

//go:generate go run github.com/cilium/ebpf/cmd/bpf2go -target bpfel -cc clang gen_tcp ./kernel/tcp.bpf.c -- -I/usr/include/bpf -I./kernel

import (
	"fmt"

	"github.com/cilium/ebpf/link"
	"golang.org/x/sys/unix"
)

type Objects struct {
	inner *gen_tcpObjects
	tp    link.Link
}

func Load() (*Objects, error) {
	if err := unix.Setrlimit(unix.RLIMIT_MEMLOCK, &unix.Rlimit{
		Cur: unix.RLIM_INFINITY,
		Max: unix.RLIM_INFINITY,
	}); err != nil {
		return nil, fmt.Errorf("setrlimit: %w", err)
	}

	objs := &gen_tcpObjects{}
	if err := loadGen_tcpObjects(objs, nil); err != nil {
		return nil, fmt.Errorf("load bpf objects: %w", err)
	}

	tp, err := link.Tracepoint("sock", "inet_sock_set_state", objs.ConsumeTcpEvent, nil)
	if err != nil {
		objs.Close()
		return nil, fmt.Errorf("attach tracepoint: %w", err)
	}

	return &Objects{inner: objs, tp: tp}, nil
}

func (o *Objects) Close() {
	o.tp.Close()
	o.inner.Close()
}
