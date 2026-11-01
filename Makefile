.PHONY: infra gossip-collector gossip-api frontend gossip-agent streaming \
        clear clear-agent clear-infra clear-streaming

COMPOSE := docker compose -f gossip-platform/docker-compose.yaml

infra:
	$(COMPOSE) up -d kafka neo4j schema-registry kafka-ui

gossip-collector:
	cd gossip-platform && go run ./cmd/collector

gossip-api:
	cd gossip-platform && NEO4J_URI=bolt://localhost:7687 NEO4J_USER=neo4j NEO4J_PASSWORD=password go run ./cmd/api

frontend:
	cd gossip-frontend && npm run dev

gossip-agent:
	make -C gossip-agent build
	cd gossip-agent && sudo ./gossip-agent kafka/kafka-config.json

streaming:
	docker compose -f streaming-service/docker-compose.yml up -d

## Clear
clear-agent:
	-sudo pkill -x gossip-agent 2>/dev/null; true

clear-infra:
	-$(COMPOSE) down -v

clear-streaming:
	-docker compose -f streaming-service/docker-compose.yml down -v

clear: clear-agent clear-infra clear-streaming
