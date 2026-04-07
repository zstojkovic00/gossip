package kafka

import (
	"fmt"

	"github.com/IBM/sarama"
	avro "github.com/hamba/avro/v2"
)

type Producer struct {
	producer   sarama.SyncProducer
	avroSchema avro.Schema
	schemaID   int
	topic      string
}

func NewProducer(cfg Config) (*Producer, error) {
	schemaID, err := resolveSchemaID(cfg.RegistryURL, cfg.Topic+"-value", tcpEventSchema)
	if err != nil {
		return nil, err
	}

	avroSchema, err := parseSchema()
	if err != nil {
		return nil, fmt.Errorf("parse avro schema: %w", err)
	}

	saramaCfg := sarama.NewConfig()
	saramaCfg.Producer.Return.Successes = true

	p, err := sarama.NewSyncProducer([]string{cfg.Broker}, saramaCfg)
	if err != nil {
		return nil, fmt.Errorf("kafka producer: %w", err)
	}

	return &Producer{
		producer:   p,
		avroSchema: avroSchema,
		schemaID:   schemaID,
		topic:      cfg.Topic,
	}, nil
}

func (p *Producer) Send(event TcpEvent) error {
	msg, err := encode(p.avroSchema, p.schemaID, event)
	if err != nil {
		return fmt.Errorf("avro encode: %w", err)
	}

	_, _, err = p.producer.SendMessage(&sarama.ProducerMessage{
		Topic: p.topic,
		Key:   sarama.StringEncoder(event.Skaddr),
		Value: sarama.ByteEncoder(msg),
	})
	if err != nil {
		return fmt.Errorf("kafka send: %w", err)
	}

	return nil
}

func (p *Producer) Close() error {
	return p.producer.Close()
}
