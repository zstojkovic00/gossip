package kafka

import (
	"fmt"

	"github.com/riferrei/srclient"
)

func resolveSchemaID(registryURL, subject, schema string) (int, error) {
	client := srclient.NewSchemaRegistryClient(registryURL)

	s, err := client.GetLatestSchema(subject)
	if err != nil {
		s, err = client.CreateSchema(subject, schema, srclient.Avro)
		if err != nil {
			return 0, fmt.Errorf("register schema: %w", err)
		}
	}

	return s.ID(), nil
}
