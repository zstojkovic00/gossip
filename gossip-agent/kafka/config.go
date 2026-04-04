package kafka

import (
	"encoding/json"
	"fmt"
	"os"
)

type Config struct {
	Broker      string `json:"broker"`
	RegistryURL string `json:"registry_url"`
	Topic       string `json:"topic"`
}

func LoadConfig(path string) (Config, error) {
	f, err := os.Open(path)
	if err != nil {
		return Config{}, fmt.Errorf("open config: %w", err)
	}
	defer f.Close()

	var cfg Config
	if err := json.NewDecoder(f).Decode(&cfg); err != nil {
		return Config{}, fmt.Errorf("decode config: %w", err)
	}
	return cfg, nil
}
