package http

//go:generate go run github.com/cilium/ebpf/cmd/bpf2go -target bpfel -cc clang gen_http ./kernel/http.bpf.c -- -I/usr/include/bpf -I../

import (
	"fmt"

	"github.com/cilium/ebpf/link"
	"golang.org/x/sys/unix"
)

type Objects struct {
	inner *gen_httpObjects
	links []link.Link
}

func Load() (*Objects, error) {
	if err := unix.Setrlimit(unix.RLIMIT_MEMLOCK, &unix.Rlimit{
		Cur: unix.RLIM_INFINITY,
		Max: unix.RLIM_INFINITY,
	}); err != nil {
		return nil, fmt.Errorf("setrlimit: %w", err)
	}

	objs := &gen_httpObjects{}
	if err := loadGen_httpObjects(objs, nil); err != nil {
		return nil, fmt.Errorf("load http bpf objects: %w", err)
	}

	names := []string{
		"sys_enter_accept4", "sys_exit_accept4",
		"sys_enter_accept", "sys_exit_accept",
		"sys_enter_read", "sys_exit_read",
		"sys_enter_write", "sys_exit_write",
		"sys_enter_close", "sys_exit_close",
	}

	var links []link.Link
	for _, name := range names {
		l, err := attach(name, objs)
		if err != nil {
			for _, opened := range links {
				opened.Close()
			}
			objs.Close()
			return nil, fmt.Errorf("attach tracepoint %s: %w", name, err)
		}
		links = append(links, l)
	}

	return &Objects{inner: objs, links: links}, nil
}

func attach(name string, objs *gen_httpObjects) (link.Link, error) {
	switch name {
	case "sys_enter_accept4":
		return link.Tracepoint("syscalls", name, objs.SysEnterAccept4, nil)
	case "sys_exit_accept4":
		return link.Tracepoint("syscalls", name, objs.SysExitAccept4, nil)
	case "sys_enter_accept":
		return link.Tracepoint("syscalls", name, objs.SysEnterAccept, nil)
	case "sys_exit_accept":
		return link.Tracepoint("syscalls", name, objs.SysExitAccept, nil)
	case "sys_enter_read":
		return link.Tracepoint("syscalls", name, objs.SysEnterRead, nil)
	case "sys_exit_read":
		return link.Tracepoint("syscalls", name, objs.SysExitRead, nil)
	case "sys_enter_write":
		return link.Tracepoint("syscalls", name, objs.SysEnterWrite, nil)
	case "sys_exit_write":
		return link.Tracepoint("syscalls", name, objs.SysExitWrite, nil)
	case "sys_enter_close":
		return link.Tracepoint("syscalls", name, objs.SysEnterClose, nil)
	case "sys_exit_close":
		return link.Tracepoint("syscalls", name, objs.SysExitClose, nil)
	default:
		return nil, fmt.Errorf("unknown probe: %s", name)
	}
}

func (o *Objects) Close() {
	for _, l := range o.links {
		l.Close()
	}
	o.inner.Close()
}
