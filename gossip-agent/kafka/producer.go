package kafka

import (
	"fmt"

	"github.com/IBM/sarama"
	avro "github.com/hamba/avro/v2"
)

type Producer[T any] struct {
	producer   sarama.SyncProducer
	avroSchema avro.Schema
	schemaID   int
	topic      string
}

func newProducer[T any](broker, topic, registryURL, schemaStr string) (*Producer[T], error) {
	schemaID, err := resolveSchemaID(registryURL, topic+"-value", schemaStr)
	if err != nil {
		return nil, err
	}

	avroSchema, err := avro.Parse(schemaStr)
	if err != nil {
		return nil, fmt.Errorf("parse avro schema: %w", err)
	}

	saramaCfg := sarama.NewConfig()
	saramaCfg.Producer.Return.Successes = true

	p, err := sarama.NewSyncProducer([]string{broker}, saramaCfg)
	if err != nil {
		return nil, fmt.Errorf("kafka producer: %w", err)
	}

	return &Producer[T]{
		producer:   p,
		avroSchema: avroSchema,
		schemaID:   schemaID,
		topic:      topic,
	}, nil
}

func NewTcpProducer(cfg Config) (*Producer[TcpEvent], error) {
	return newProducer[TcpEvent](cfg.Broker, cfg.TcpTopic, cfg.RegistryURL, tcpEventSchema)
}

func NewHttpProducer(cfg Config) (*Producer[HttpEvent], error) {
	return newProducer[HttpEvent](cfg.Broker, cfg.HttpTopic, cfg.RegistryURL, httpEventSchema)
}

func (p *Producer[T]) Send(event T) error {
	msg, err := encode(p.avroSchema, p.schemaID, event)
	if err != nil {
		return fmt.Errorf("avro encode: %w", err)
	}

	_, _, err = p.producer.SendMessage(&sarama.ProducerMessage{
		Topic: p.topic,
		Value: sarama.ByteEncoder(msg),
	})
	if err != nil {
		return fmt.Errorf("kafka send: %w", err)
	}

	return nil
}

func (p *Producer[T]) Close() error {
	return p.producer.Close()
}
