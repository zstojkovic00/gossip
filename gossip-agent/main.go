package main

import (
	"log"
	"os"

	"gossip-agent/ebpf"
	"gossip-agent/kafka"
)

func main() {
	if len(os.Args) < 2 {
		log.Fatalf("usage: %s <config.json>", os.Args[0])
	}

	cfg, err := kafka.LoadConfig(os.Args[1])
	if err != nil {
		log.Fatalf("config: %v", err)
	}

	// TCP
	objs, err := ebpf.Load()
	if err != nil {
		log.Fatalf("ebpf load: %v", err)
	}
	defer objs.Close()

	listener, err := ebpf.NewListener(objs)
	if err != nil {
		log.Fatalf("ebpf listener: %v", err)
	}
	defer listener.Close()

	producer, err := kafka.NewProducer(cfg)
	if err != nil {
		log.Fatalf("kafka producer: %v", err)
	}
	defer producer.Close()

	httpObjs, err := ebpf.LoadHTTP()
	if err != nil {
		log.Fatalf("ebpf http load: %v", err)
	}
	defer httpObjs.Close()

	httpListener, err := ebpf.NewHTTPListener(httpObjs)
	if err != nil {
		log.Fatalf("ebpf http listener: %v", err)
	}
	defer httpListener.Close()

	go listenHTTPOpen(httpListener)
	go listenHTTPData(httpListener)
	go listenHTTPClose(httpListener)

	log.Println("gossip-agent started")

	for {
		event, err := listener.Read()
		if err != nil {
			log.Printf("read: %v", err)
			continue
		}

		if err := producer.Send(kafka.TcpEvent{
			Skaddr:   event.Skaddr,
			Pid:      event.Pid,
			Saddr:    event.Saddr,
			Daddr:    event.Daddr,
			Sport:    event.Sport,
			Dport:    event.Dport,
			NewState: event.NewState,
			OldState: event.OldState,
			Comm:     event.Comm,
		}); err != nil {
			log.Printf("send: %v", err)
			continue
		}

		log.Printf("skaddr=%-18s pid=%-6d comm=%-16s %s:%-5d → %s:%-5d state=%s",
			event.Skaddr,
			event.Pid, event.Comm,
			event.Saddr, event.Sport,
			event.Daddr, event.Dport,
			event.NewState)
	}
}

func listenHTTPOpen(l *ebpf.HTTPListener) {
	for {
		e, err := l.ReadOpen()
		if err != nil {
			log.Printf("http open: %v", err)
			return
		}
		log.Printf("[HTTP open]  pid=%-6d fd=%-4d remote=%s:%d",
			e.ConnId.Pid, e.ConnId.Fd, e.RemoteAddr, e.RemotePort)
	}
}

func listenHTTPData(l *ebpf.HTTPListener) {
	for {
		e, err := l.ReadData()
		if err != nil {
			log.Printf("http data: %v", err)
			return
		}
		preview := e.Msg
		if len(preview) > 120 {
			preview = preview[:120]
		}
		log.Printf("[HTTP data]  pid=%-6d fd=%-4d dir=%-7s pos=%-6d msg=%q",
			e.ConnId.Pid, e.ConnId.Fd, e.Direction, e.Pos, preview)
	}
}

func listenHTTPClose(l *ebpf.HTTPListener) {
	for {
		e, err := l.ReadClose()
		if err != nil {
			log.Printf("http close: %v", err)
			return
		}
		log.Printf("[HTTP close] pid=%-6d fd=%-4d wr=%-8d rd=%d",
			e.ConnId.Pid, e.ConnId.Fd, e.WrBytes, e.RdBytes)
	}
}
