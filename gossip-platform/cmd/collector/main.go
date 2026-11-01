package main

import (
	"context"
	"log"
	"os"
	"sync"

	"gossip-platform/internal/kafka"
)

func main() {
	if len(os.Args) < 2 {
		log.Fatalf("usage: %s <config.json>", os.Args[0])
	}

	cfg, err := kafka.LoadConfig(os.Args[1])
	if err != nil {
		log.Fatalf("config: %v", err)
	}

	tcpConsumer, err := kafka.NewTcpConsumer(cfg, handleTCP)
	if err != nil {
		log.Fatalf("kafka tcp consumer: %v", err)
	}
	defer tcpConsumer.Close()

	httpConsumer, err := kafka.NewHttpConsumer(cfg, handleHTTP)
	if err != nil {
		log.Fatalf("kafka http consumer: %v", err)
	}
	defer httpConsumer.Close()

	log.Println("gossip-collector started")

	ctx := context.Background()
	var wg sync.WaitGroup

	wg.Add(1)
	go func() {
		defer wg.Done()
		if err := tcpConsumer.Run(ctx); err != nil {
			log.Printf("tcp: %v", err)
		}
	}()

	wg.Add(1)
	go func() {
		defer wg.Done()
		if err := httpConsumer.Run(ctx); err != nil {
			log.Printf("http: %v", err)
		}
	}()

	wg.Wait()
}

func handleTCP(e kafka.TcpEvent) error {
	log.Printf("[TCP] pid=%-6d comm=%-16s %s:%-5d → %s:%-5d ts=%d",
		e.Pid, e.Comm, e.Saddr, e.Sport, e.Daddr, e.Dport, e.Timestamp)
	return nil
}

func handleHTTP(e kafka.HttpEvent) error {
	log.Printf("[HTTP] %s:%-5d → %s:%-5d %s %s → %d ts=%d",
		e.Saddr, e.Sport, e.Daddr, e.Dport, e.Method, e.URL, e.Status, e.Timestamp)
	return nil
}
