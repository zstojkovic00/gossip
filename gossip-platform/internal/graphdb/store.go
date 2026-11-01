package graphdb

import (
	"context"
	"errors"

	"github.com/neo4j/neo4j-go-driver/v5/neo4j"
)

type Store struct {
	driver neo4j.DriverWithContext
}

func NewStore(driver neo4j.DriverWithContext) *Store {
	return &Store{driver: driver}
}

func (s *Store) Topology(ctx context.Context) (Topology, error) {
	result, err := neo4j.ExecuteQuery(ctx, s.driver, topologyQuery, nil,
		neo4j.EagerResultTransformer, neo4j.ExecuteQueryWithReadersRouting())
	if err != nil {
		return Topology{}, err
	}
	if len(result.Records) == 0 {
		return Topology{}, errors.New("topology query returned no rows")
	}

	record := result.Records[0]
	return Topology{
		Services:     list(record, "services", toService),
		Connections:  list(record, "connections", toConnection),
		Interactions: list(record, "interactions", toInteraction),
	}, nil
}
