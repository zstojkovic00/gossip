.PHONY: infra gossip-collector gossip-api frontend gossip-agent streaming \
        clear clear-agent clear-infra clear-streaming

COMPOSE := docker compose -f gossip-collector/docker-compose.yaml

infra:
	$(COMPOSE) up -d kafka neo4j schema-registry kafka-ui

gossip-collector:
	cd gossip-collector && ./gradlew bootRun

gossip-api:
	cd gossip-api && ./gradlew bootRun

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
