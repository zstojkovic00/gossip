package kafka

import (
	"fmt"
	"sync"

	avro "github.com/hamba/avro/v2"
	"github.com/riferrei/srclient"
)

type schemaRegistry struct {
	client *srclient.SchemaRegistryClient
	mu     sync.Mutex
	cache  map[int]avro.Schema
	reader avro.Schema
}

func newSchemaRegistry(registryURL, readerSchema string) (*schemaRegistry, error) {
	reader, err := avro.Parse(readerSchema)
	if err != nil {
		return nil, fmt.Errorf("parse reader schema: %w", err)
	}

	return &schemaRegistry{
		client: srclient.NewSchemaRegistryClient(registryURL),
		cache:  make(map[int]avro.Schema),
		reader: reader,
	}, nil
}

func (r *schemaRegistry) get(id int) (avro.Schema, error) {
	r.mu.Lock()
	defer r.mu.Unlock()

	if schema, ok := r.cache[id]; ok {
		return schema, nil
	}

	s, err := r.client.GetSchema(id)
	if err != nil {
		return nil, fmt.Errorf("get schema %d: %w", id, err)
	}

	schema, err := avro.Parse(s.Schema())
	if err != nil {
		return nil, fmt.Errorf("parse avro schema %d: %w", id, err)
	}
	resolved, err := avro.NewSchemaCompatibility().Resolve(r.reader, schema)
	if err != nil {
		return nil, fmt.Errorf("resolve avro schema %d: %w", id, err)
	}

	r.cache[id] = resolved
	return resolved, nil
}
