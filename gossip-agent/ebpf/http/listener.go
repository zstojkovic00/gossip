package http

import (
	"bytes"
	"encoding/binary"
	"fmt"

	"github.com/cilium/ebpf/ringbuf"
)

type Listener struct {
	rd *ringbuf.Reader
}

func NewListener(objs *Objects) (*Listener, error) {
	rd, err := ringbuf.NewReader(objs.inner.SocketDataEvents)
	if err != nil {
		return nil, fmt.Errorf("data events ringbuf: %w", err)
	}
	return &Listener{rd: rd}, nil
}

func (l *Listener) Read() (DataEvent, error) {
	record, err := l.rd.Read()
	if err != nil {
		return DataEvent{}, fmt.Errorf("read data event: %w", err)
	}

	var raw rawDataEvent
	if err := binary.Read(bytes.NewBuffer(record.RawSample), binary.LittleEndian, &raw); err != nil {
		return DataEvent{}, fmt.Errorf("parse data event: %w", err)
	}

	return parseDataEvent(raw), nil
}

func (l *Listener) Close() {
	l.rd.Close()
}
