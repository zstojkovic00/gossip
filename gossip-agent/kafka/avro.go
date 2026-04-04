package kafka

import (
	_ "embed"
	"encoding/binary"

	avro "github.com/hamba/avro/v2"
)

//go:embed schemas/tcp_event.avsc
var tcpEventSchema string

func parseSchema() (avro.Schema, error) {
	return avro.Parse(tcpEventSchema)
}

// Confluent's Schema Registry wire format: [0x00 magic byte][schema ID 4B big-endian][avro payload]
func encode(schema avro.Schema, schemaID int, event TcpEvent) ([]byte, error) {
	avroBytes, err := avro.Marshal(schema, event)
	if err != nil {
		return nil, err
	}

	buf := make([]byte, 5+len(avroBytes))
	buf[0] = 0x00
	binary.BigEndian.PutUint32(buf[1:5], uint32(schemaID))
	copy(buf[5:], avroBytes)
	return buf, nil
}
