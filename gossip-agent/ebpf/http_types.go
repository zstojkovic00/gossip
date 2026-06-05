package ebpf

import (
	"encoding/binary"
	"net"
)

const maxHTTPMsgSize = 30720

type rawConnId struct {
	Pid  uint32
	Fd   uint32
	Tsid uint64
}
type rawSockaddrIn struct {
	Family uint16
	Port   [2]byte
	Addr   [4]byte
	Pad    [8]byte
}

type rawOpenEvent struct {
	TimestampNs uint64
	ConnId      rawConnId
	Addr        rawSockaddrIn
}

type rawCloseEvent struct {
	TimestampNs uint64
	ConnId      rawConnId
	WrBytes     uint64
	RdBytes     uint64
}

type rawDataAttr struct {
	TimestampNs uint64
	ConnIdPid   uint32
	ConnIdFd    uint32
	ConnIdTsid  uint64
	Direction   uint32
	Pad         [4]byte
	MsgSize     uint64
	Pos         uint64
}

type rawDataEvent struct {
	Attr rawDataAttr
	Msg  [maxHTTPMsgSize]byte
}

type ConnId struct {
	Pid  uint32
	Fd   uint32
	Tsid uint64
}

type OpenEvent struct {
	TimestampNs uint64
	ConnId      ConnId
	RemoteAddr  string
	RemotePort  uint16
}

type CloseEvent struct {
	TimestampNs uint64
	ConnId      ConnId
	WrBytes     uint64
	RdBytes     uint64
}

type DataEvent struct {
	TimestampNs uint64
	ConnId      ConnId
	Direction   string
	Pos         uint64
	Msg         string
}

func parseOpenEvent(raw rawOpenEvent) OpenEvent {
	return OpenEvent{
		TimestampNs: raw.TimestampNs,
		ConnId:      ConnId{Pid: raw.ConnId.Pid, Fd: raw.ConnId.Fd, Tsid: raw.ConnId.Tsid},
		RemoteAddr:  net.IP(raw.Addr.Addr[:]).String(),
		RemotePort:  binary.BigEndian.Uint16(raw.Addr.Port[:]),
	}
}
func parseCloseEvent(raw rawCloseEvent) CloseEvent {
	return CloseEvent{
		TimestampNs: raw.TimestampNs,
		ConnId:      ConnId{Pid: raw.ConnId.Pid, Fd: raw.ConnId.Fd, Tsid: raw.ConnId.Tsid},
		WrBytes:     raw.WrBytes,
		RdBytes:     raw.RdBytes,
	}
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
		ConnId:      ConnId{Pid: raw.Attr.ConnIdPid, Fd: raw.Attr.ConnIdFd, Tsid: raw.Attr.ConnIdTsid},
		Direction:   direction,
		Pos:         raw.Attr.Pos,
		Msg:         string(raw.Msg[:size]),
	}
}
