package ebpf

import (
	"encoding/binary"
	"fmt"
	"net"
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

type rawEvent struct {
	Skaddr   uint64
	Pid      uint32
	Saddr    uint32
	Daddr    uint32
	OldState uint32
	NewState uint32
	Sport    uint16
	Dport    uint16
	Comm     [16]byte
}
type Event struct {
	Skaddr   string
	Pid      int32
	Saddr    string
	Daddr    string
	OldState string
	NewState string
	Sport    int32
	Dport    int32
	Comm     string
}

func parseEvent(raw rawEvent) Event {
	return Event{
		Skaddr:   fmt.Sprintf("0x%x", raw.Skaddr),
		Pid:      int32(raw.Pid),
		Saddr:    toIPv4(raw.Saddr).String(),
		Daddr:    toIPv4(raw.Daddr).String(),
		Sport:    int32(raw.Sport),
		Dport:    int32(raw.Dport),
		OldState: toTcpState(raw.OldState),
		NewState: toTcpState(raw.NewState),
		Comm:     nullTerminatedString(raw.Comm[:]),
	}
}

func toIPv4(ip uint32) net.IP {
	addr := make(net.IP, 4)
	binary.LittleEndian.PutUint32(addr, ip)
	return addr
}

func nullTerminatedString(b []byte) string {
	for i, c := range b {
		if c == 0 {
			return string(b[:i])
		}
	}
	return string(b)
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
