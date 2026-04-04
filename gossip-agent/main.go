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

	log.Println("gossip-agent started")

	for {
		event, err := listener.Read()
		if err != nil {
			log.Printf("read: %v", err)
			continue
		}

		if err := producer.Send(kafka.TcpEvent{
			Pid:   event.Pid,
			Saddr: event.Saddr,
			Daddr: event.Daddr,
			Sport: event.Sport,
			Dport: event.Dport,
			State: event.State,
			Comm:  event.Comm,
		}); err != nil {
			log.Printf("send: %v", err)
			continue
		}

		log.Printf("pid=%-6d comm=%-16s %s:%-5d → %s:%-5d state=%s",
			event.Pid, event.Comm,
			event.Saddr, event.Sport,
			event.Daddr, event.Dport,
			event.State)
	}
}
