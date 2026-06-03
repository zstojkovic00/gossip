.PHONY: gossip-agent gossip-collector demo run clear-demo clear-agent clear-collector clear

gossip-collector:
	@echo "[gossip-collector]"
	docker compose -f gossip-collector/docker-compose.yaml up -d

gossip-agent:
	@echo "[gossip-agent]"
	make -C gossip-agent run

run: gossip-collector gossip-agent

clear-agent:
	@echo "[clear-agent]"
	sudo kill $$(pgrep tcp_listener) 2>/dev/null

clear-collector:
	@echo "[clear-collector]"
	docker compose -f gossip-collector/docker-compose.yaml down -v
	docker compose -f gossip-collector/docker-compose.yaml up -d
ne
clear: clear-collector clear-agent

demo:
	@echo "Starting HTTP server on port 8085..."
	python3 -m http.server 8085 &>/dev/null &
	@echo "Sending requests every 3 seconds. Press Ctrl+C to stop."
	while true; do curl -s http://localhost:8085 > /dev/null; sleep 15; done


clear-demo:
	@echo "[clear-demo]"
	pkill -f "python3 -m http.server" 2>/dev/null