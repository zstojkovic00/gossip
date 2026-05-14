.PHONY: infra gossip-agent gossip-collector

gossip-collector:
	docker compose -f gossip-collector/docker-compose.yaml up -d

gossip-agent:
	make -C gossip-agent run