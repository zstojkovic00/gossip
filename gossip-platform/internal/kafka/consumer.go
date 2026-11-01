package kafka

import (
	"context"
	"fmt"
	"log"

	"github.com/IBM/sarama"
)

type Consumer[T any] struct {
	group    sarama.ConsumerGroup
	registry *schemaRegistry
	topic    string
	handle   func(T) error
}

func newConsumer[T any](broker, topic, registryURL, readerSchema string, handle func(T) error) (*Consumer[T], error) {
	saramaCfg := sarama.NewConfig()
	saramaCfg.Consumer.Offsets.Initial = sarama.OffsetOldest

	group, err := sarama.NewConsumerGroup([]string{broker}, "gossip-collector", saramaCfg)
	if err != nil {
		return nil, fmt.Errorf("kafka consumer group: %w", err)
	}

	registry, err := newSchemaRegistry(registryURL, readerSchema)
	if err != nil {
		return nil, err
	}

	return &Consumer[T]{
		group:    group,
		registry: registry,
		topic:    topic,
		handle:   handle,
	}, nil
}

func NewTcpConsumer(cfg Config, handle func(TcpEvent) error) (*Consumer[TcpEvent], error) {
	return newConsumer(cfg.Broker, cfg.TcpTopic, cfg.RegistryURL, tcpEventSchema, handle)
}

func NewHttpConsumer(cfg Config, handle func(HttpEvent) error) (*Consumer[HttpEvent], error) {
	return newConsumer(cfg.Broker, cfg.HttpTopic, cfg.RegistryURL, httpEventSchema, handle)
}

func (c *Consumer[T]) Run(ctx context.Context) error {
	for {
		if err := c.group.Consume(ctx, []string{c.topic}, c); err != nil {
			return fmt.Errorf("kafka consume: %w", err)
		}
		if ctx.Err() != nil {
			return nil
		}
	}
}

func (c *Consumer[T]) Close() error {
	return c.group.Close()
}

func (c *Consumer[T]) Setup(sarama.ConsumerGroupSession) error   { return nil }
func (c *Consumer[T]) Cleanup(sarama.ConsumerGroupSession) error { return nil }

func (c *Consumer[T]) ConsumeClaim(session sarama.ConsumerGroupSession, claim sarama.ConsumerGroupClaim) error {
	for msg := range claim.Messages() {
		event, err := decode[T](c.registry, msg.Value)
		if err != nil {
			log.Printf("%s offset=%d avro decode: %v", c.topic, msg.Offset, err)
			session.MarkMessage(msg, "")
			continue
		}

		if err := c.handle(event); err != nil {
			return fmt.Errorf("%s offset=%d handle: %w", c.topic, msg.Offset, err)
		}

		session.MarkMessage(msg, "")
	}
	return nil
}
