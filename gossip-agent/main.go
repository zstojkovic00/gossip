package main

import (
	"fmt"
	"log"
	"os"
	"strings"
	"sync"
	"time"

	httpebpf "gossip-agent/ebpf/http"
	"gossip-agent/ebpf/tcp"
	"gossip-agent/kafka"

	"golang.org/x/sys/unix"
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
	tcpObjs, err := tcp.Load()
	if err != nil {
		log.Fatalf("ebpf tcp load: %v", err)
	}
	defer tcpObjs.Close()

	tcpListener, err := tcp.NewListener(tcpObjs)
	if err != nil {
		log.Fatalf("ebpf tcp listener: %v", err)
	}
	defer tcpListener.Close()

	tcpProducer, err := kafka.NewTcpProducer(cfg)
	if err != nil {
		log.Fatalf("kafka tcp producer: %v", err)
	}
	defer tcpProducer.Close()

	// HTTP
	httpObjs, err := httpebpf.Load()
	if err != nil {
		log.Fatalf("ebpf http load: %v", err)
	}
	defer httpObjs.Close()

	httpListener, err := httpebpf.NewListener(httpObjs)
	if err != nil {
		log.Fatalf("ebpf http listener: %v", err)
	}
	defer httpListener.Close()

	httpProducer, err := kafka.NewHttpProducer(cfg)
	if err != nil {
		log.Fatalf("kafka http producer: %v", err)
	}
	defer httpProducer.Close()

	log.Println("gossip-agent started")

	var wg sync.WaitGroup

	wg.Add(1)
	go func() {
		defer wg.Done()
		listenTCP(tcpListener, tcpProducer)
	}()

	wg.Add(1)
	go func() {
		defer wg.Done()
		listenHTTP(httpListener, httpProducer)
	}()

	wg.Wait()
}

func listenTCP(l *tcp.Listener, p *kafka.Producer[kafka.TcpEvent]) {
	for {
		event, err := l.Read()
		if err != nil {
			log.Printf("tcp read: %v", err)
			return
		}

		if event.OldState != "SYN_SENT" || event.NewState != "ESTABLISHED" {
			continue
		}

		if err := p.Send(kafka.TcpEvent{
			Skaddr:    event.Skaddr,
			Pid:       event.Pid,
			Saddr:     event.Saddr,
			Daddr:     event.Daddr,
			Sport:     event.Sport,
			Dport:     event.Dport,
			NewState:  event.NewState,
			OldState:  event.OldState,
			Comm:      event.Comm,
			Timestamp: toUnixMilli(event.TimestampNs),
		}); err != nil {
			log.Printf("tcp send: %v", err)
			continue
		}

		log.Printf("[TCP] pid=%-6d comm=%-16s %s:%-5d → %s:%-5d state=%s",
			event.Pid, event.Comm,
			event.Saddr, event.Sport,
			event.Daddr, event.Dport,
			event.NewState)
	}
}

func listenHTTP(l *httpebpf.Listener, p *kafka.Producer[kafka.HttpEvent]) {
	pending := make(map[string][]httpebpf.Request)

	for {
		e, err := l.Read()
		if err != nil {
			log.Printf("http read: %v", err)
			return
		}

		conn := fmt.Sprintf("%d-%d-%d", e.Pid, e.Fd, e.Tsid)

		log.Printf("[RAW] conn=%s %-7s size=%-6d %s:%d → %s:%d %q",
			conn, e.Direction, len(e.Msg), e.Saddr, e.Sport, e.Daddr, e.Dport, firstLine(e.Msg))

		if e.Saddr == "0.0.0.0" || e.Daddr == "0.0.0.0" {
			continue
		}

		if e.Direction == "ingress" {
			req, ok := httpebpf.ParseRequest(e.Msg)
			if !ok {
				continue
			}
			pending[conn] = append(pending[conn], req)
		} else {
			resp, ok := httpebpf.ParseResponse(e.Msg)
			if !ok {
				continue
			}
			queue := pending[conn]
			if len(queue) == 0 {
				log.Printf("[RAW] conn=%s response %d without a pending request", conn, resp.Status)
				continue
			}
			req := queue[0]
			if len(queue) == 1 {
				delete(pending, conn)
			} else {
				pending[conn] = queue[1:]
			}

			if err := p.Send(kafka.HttpEvent{
				Skaddr:    e.Skaddr,
				Saddr:     e.Saddr,
				Daddr:     e.Daddr,
				Sport:     e.Sport,
				Dport:     e.Dport,
				Method:    req.Method,
				URL:       req.URL,
				Status:    int32(resp.Status),
				Timestamp: toUnixMilli(e.TimestampNs),
			}); err != nil {
				log.Printf("http send: %v", err)
				continue
			}

			log.Printf("[HTTP] %s:%-5d → %s:%-5d %s %s → %d",
				e.Saddr, e.Sport, e.Daddr, e.Dport, req.Method, req.URL, resp.Status)
		}
	}
}

var bootTime = func() time.Time {
	var ts unix.Timespec
	if err := unix.ClockGettime(unix.CLOCK_BOOTTIME, &ts); err != nil {
		log.Fatalf("clock_gettime: %v", err)
	}
	return time.Now().Add(-time.Duration(ts.Nano()))
}()

func toUnixMilli(bootNs uint64) int64 {
	return bootTime.Add(time.Duration(bootNs)).UnixMilli()
}

func firstLine(msg string) string {
	line, _, _ := strings.Cut(msg, "\r\n")
	if len(line) > 100 {
		return line[:100]
	}
	return line
}
