.PHONY: infra gossip-agent gossip-collector

infra:
	docker compose -f infra/kafka/docker-compose.yaml up -d
	docker compose -f infra/neo4j/docker-compose.yaml up -d

gossip-agent:
	make -C gossip-agent run

gossip-collector:
	./gossip-collector/gradlew -p gossip-collector bootRun