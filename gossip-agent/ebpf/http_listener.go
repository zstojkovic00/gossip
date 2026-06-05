package ebpf

import (
	"bytes"
	"encoding/binary"
	"fmt"

	"github.com/cilium/ebpf/ringbuf"
)

type HTTPListener struct {
	openRd  *ringbuf.Reader
	dataRd  *ringbuf.Reader
	closeRd *ringbuf.Reader
}

func NewHTTPListener(objs *HTTPObjects) (*HTTPListener, error) {
	openRd, err := ringbuf.NewReader(objs.inner.SocketOpenEvents)
	if err != nil {
		return nil, fmt.Errorf("open events ringbuf: %w", err)
	}

	dataRd, err := ringbuf.NewReader(objs.inner.SocketDataEvents)
	if err != nil {
		openRd.Close()
		return nil, fmt.Errorf("data events ringbuf: %w", err)
	}

	closeRd, err := ringbuf.NewReader(objs.inner.SocketCloseEvents)
	if err != nil {
		openRd.Close()
		dataRd.Close()
		return nil, fmt.Errorf("close events ringbuf: %w", err)
	}

	return &HTTPListener{openRd: openRd, dataRd: dataRd, closeRd: closeRd}, nil
}
func (l *HTTPListener) ReadOpen() (OpenEvent, error) {
	record, err := l.openRd.Read()
	if err != nil {
		return OpenEvent{}, fmt.Errorf("read open event: %w", err)
	}

	var raw rawOpenEvent
	if err := binary.Read(bytes.NewBuffer(record.RawSample), binary.LittleEndian, &raw); err != nil {
		return OpenEvent{}, fmt.Errorf("parse open event: %w", err)
	}

	return parseOpenEvent(raw), nil
}
func (l *HTTPListener) ReadData() (DataEvent, error) {
	record, err := l.dataRd.Read()
	if err != nil {
		return DataEvent{}, fmt.Errorf("read data event: %w", err)
	}

	var raw rawDataEvent
	if err := binary.Read(bytes.NewBuffer(record.RawSample), binary.LittleEndian, &raw); err != nil {
		return DataEvent{}, fmt.Errorf("parse data event: %w", err)
	}

	return parseDataEvent(raw), nil
}
func (l *HTTPListener) ReadClose() (CloseEvent, error) {
	record, err := l.closeRd.Read()
	if err != nil {
		return CloseEvent{}, fmt.Errorf("read close event: %w", err)
	}

	var raw rawCloseEvent
	if err := binary.Read(bytes.NewBuffer(record.RawSample), binary.LittleEndian, &raw); err != nil {
		return CloseEvent{}, fmt.Errorf("parse close event: %w", err)
	}

	return parseCloseEvent(raw), nil
}

func (l *HTTPListener) Close() {
	l.openRd.Close()
	l.dataRd.Close()
	l.closeRd.Close()
}
