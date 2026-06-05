package ebpf

//go:generate go run github.com/cilium/ebpf/cmd/bpf2go -target bpfel -cc clang gen_tcp ./kernel/tcp.bpf.c -- -I/usr/include/bpf -I./kernel
//go:generate go run github.com/cilium/ebpf/cmd/bpf2go -target bpfel -cc clang gen_http ./kernel/http.bpf.c -- -I/usr/include/bpf -I./kernel

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

type HTTPObjects struct {
	inner *gen_httpObjects
	links []link.Link
}

func LoadHTTP() (*HTTPObjects, error) {
	objs := &gen_httpObjects{}
	if err := loadGen_httpObjects(objs, nil); err != nil {
		return nil, fmt.Errorf("load http bpf objects: %w", err)
	}

	type probe struct {
		group string
		name  string
	}
	probes := []probe{
		{"syscalls", "sys_enter_accept4"},
		{"syscalls", "sys_exit_accept4"},
		{"syscalls", "sys_enter_read"},
		{"syscalls", "sys_exit_read"},
		{"syscalls", "sys_enter_write"},
		{"syscalls", "sys_exit_write"},
		{"syscalls", "sys_enter_close"},
		{"syscalls", "sys_exit_close"},
	}

	var links []link.Link
	for _, p := range probes {
		l, err := httpAttach(p.group, p.name, objs)
		if err != nil {
			for _, opened := range links {
				opened.Close()
			}
			objs.Close()
			return nil, fmt.Errorf("attach tracepoint %s: %w", p.name, err)
		}
		links = append(links, l)
	}

	return &HTTPObjects{inner: objs, links: links}, nil
}

// httpAttach kači jedan tracepoint na odgovarajući eBPF program.
// Switch je neophodan jer link.Tracepoint prima *ebpf.Program, ne interface.
func httpAttach(group, name string, objs *gen_httpObjects) (link.Link, error) {
	switch name {
	case "sys_enter_accept4":
		return link.Tracepoint(group, name, objs.SysEnterAccept4, nil)
	case "sys_exit_accept4":
		return link.Tracepoint(group, name, objs.SysExitAccept4, nil)
	case "sys_enter_read":
		return link.Tracepoint(group, name, objs.SysEnterRead, nil)
	case "sys_exit_read":
		return link.Tracepoint(group, name, objs.SysExitRead, nil)
	case "sys_enter_write":
		return link.Tracepoint(group, name, objs.SysEnterWrite, nil)
	case "sys_exit_write":
		return link.Tracepoint(group, name, objs.SysExitWrite, nil)
	case "sys_enter_close":
		return link.Tracepoint(group, name, objs.SysEnterClose, nil)
	case "sys_exit_close":
		return link.Tracepoint(group, name, objs.SysExitClose, nil)
	default:
		return nil, fmt.Errorf("unknown probe: %s", name)
	}
}

func (o *HTTPObjects) Close() {
	for _, l := range o.links {
		l.Close()
	}
	o.inner.Close()
}
