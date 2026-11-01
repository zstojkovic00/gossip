package kafka

import (
	_ "embed"
	"encoding/binary"
	"fmt"

	avro "github.com/hamba/avro/v2"
)

//go:embed schemas/tcp_event.avsc
var tcpEventSchema string

//go:embed schemas/http_event.avsc
var httpEventSchema string

// Confluent's Schema Registry wire protocol format: [0x00 magic byte][schema ID 4B big-endian][avro payload]
func decode[T any](registry *schemaRegistry, msg []byte) (T, error) {
	var event T
	if len(msg) < 5 || msg[0] != 0x00 {
		return event, fmt.Errorf("not in schema registry wire format")
	}

	schema, err := registry.get(int(binary.BigEndian.Uint32(msg[1:5])))
	if err != nil {
		return event, err
	}

	if err := avro.Unmarshal(schema, msg[5:], &event); err != nil {
		return event, err
	}
	return event, nil
}
