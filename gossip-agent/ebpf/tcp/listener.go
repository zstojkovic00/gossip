package tcp

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
	rd, err := ringbuf.NewReader(objs.inner.Events)
	if err != nil {
		return nil, fmt.Errorf("ringbuf reader: %w", err)
	}
	return &Listener{rd: rd}, nil
}

func (l *Listener) Read() (Event, error) {
	record, err := l.rd.Read()
	if err != nil {
		return Event{}, fmt.Errorf("ringbuf read: %w", err)
	}

	var raw rawEvent
	if err := binary.Read(bytes.NewBuffer(record.RawSample), binary.LittleEndian, &raw); err != nil {
		return Event{}, fmt.Errorf("parse event: %w", err)
	}

	return parseEvent(raw), nil
}

func (l *Listener) Close() {
	l.rd.Close()
}
