.PHONY: gossip-agent gossip-collector streaming frontend demo run clear-demo clear-agent clear-collector clear-streaming-service clear

## RUN
gossip-collector:
	@echo "[gossip-collector]"
	docker compose -f gossip-collector/docker-compose.yaml up -d

gossip-agent:
	@echo "[gossip-agent]"
	make -C gossip-agent run

frontend:
	@echo "[gossip-frontend"]
	cd gossip-frontend && npm run dev

streaming:
	@echo "[streaming-service]"
	docker compose -f streaming-service/docker-compose.yml up -d

run: gossip-collector gossip-agent

# CLEAR
clear-agent:
	@echo "[clear-agent]"
	sudo kill $$(pgrep gossip-agent) 2>/dev/null

clear-collector:
	@echo "[clear-collector]"
	docker compose -f gossip-collector/docker-compose.yaml down -v
	docker compose -f gossip-collector/docker-compose.yaml up -d

clear-frontend:
	@echo "[clear-frontend]"
	pkill -f "npm run dev" 2>/dev/null

clear-streaming:
	@echo "[clear-streaming-service]"
	docker compose -f streaming-service/docker-compose.yml down -v

clear: clear-collector clear-streaming clear-frontend clear-agent

## ...
demo:
	@echo "Starting HTTP server on port 8085..."
	python3 -m http.server 8085 &>/dev/null &
	@echo "Sending requests every 3 seconds. Press Ctrl+C to stop."
	while true; do curl -s http://localhost:8085 > /dev/null; sleep 15; done

clear-demo:
	@echo "[clear-demo]"
	pkill -f "python3 -m http.server" 2>/dev/null