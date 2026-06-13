package http

import (
	"encoding/binary"
	"fmt"
	"net"
)

const maxHTTPMsgSize = 30720

type rawDataAttr struct {
	TimestampNs uint64
	ConnIdPid   uint32
	ConnIdFd    uint32
	ConnIdTsid  uint64
	Direction   uint32
	Pad         [4]byte
	MsgSize     uint64
	Pos         uint64
	Skaddr      uint64
	Saddr       uint32
	Daddr       uint32
	Sport       uint16
	Dport       uint16
	Pad2        [4]byte
}

type rawDataEvent struct {
	Attr rawDataAttr
	Msg  [maxHTTPMsgSize]byte
}

type DataEvent struct {
	TimestampNs uint64
	Pid         uint32
	Fd          uint32
	Tsid        uint64
	Direction   string
	Pos         uint64
	Skaddr      string
	Saddr       string
	Daddr       string
	Sport       int32
	Dport       int32
	Msg         string
}

func parseDataEvent(raw rawDataEvent) DataEvent {
	direction := "ingress"
	if raw.Attr.Direction == 0 {
		direction = "egress"
	}

	size := raw.Attr.MsgSize
	if size > maxHTTPMsgSize {
		size = maxHTTPMsgSize
	}

	return DataEvent{
		TimestampNs: raw.Attr.TimestampNs,
		Pid:         raw.Attr.ConnIdPid,
		Fd:          raw.Attr.ConnIdFd,
		Tsid:        raw.Attr.ConnIdTsid,
		Direction:   direction,
		Pos:         raw.Attr.Pos,
		Skaddr:      fmt.Sprintf("0x%x", raw.Attr.Skaddr),
		Saddr:       toIPv4(raw.Attr.Saddr),
		Daddr:       toIPv4(raw.Attr.Daddr),
		Sport:       int32(raw.Attr.Sport),
		Dport:       int32(raw.Attr.Dport),
		Msg:         string(raw.Msg[:size]),
	}
}

func toIPv4(ip uint32) string {
	b := make([]byte, 4)
	binary.LittleEndian.PutUint32(b, ip)
	return net.IP(b).String()
}
