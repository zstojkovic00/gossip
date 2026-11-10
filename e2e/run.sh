#!/usr/bin/env bash
# Usage: ./e2e/run.sh [rounds]
# Runs the README steps from a clean state: infrastructure, collector, API, agent, streaming-service and traffic.
# Everything keeps running afterwards; stop it with `make clear`.

set -euo pipefail

ROUNDS="${1:-3}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOGS="$ROOT/logs"
cd "$ROOT"

step() { printf '\n==> %s\n' "$*" >&2; }

fail() {
    echo "FAILED: $1" >&2
    [ -n "${2:-}" ] && tail -n 30 "$2" >&2
    exit 1
}

# wait_for <description> <timeout seconds> <command...>
wait_for() {
    local description="$1" timeout="$2"
    shift 2
    for _ in $(seq 1 "$timeout"); do
        "$@" > /dev/null 2>&1 && return 0
        sleep 1
    done
    fail "$description not ready after ${timeout}s" "${LOG:-}"
}

# start <name> <command...>: runs in the background, detached from this terminal
start() {
    local name="$1"
    shift
    setsid "$@" > "$LOGS/$name.log" 2>&1 < /dev/null &
}

http_ok() { [ "$(curl -s -o /dev/null -w '%{http_code}' "$1")" = "200" ]; }
port_open() { (echo > "/dev/tcp/localhost/$1") 2> /dev/null; }
neo4j_ready() { docker exec gossip-collector-neo4j-1 cypher-shell -u neo4j -p password "RETURN 1"; }

step "Asking for sudo (agent)"
sudo -v

step "Cleaning previous run"
make -s clear > /dev/null 2>&1 || true
mkdir -p "$LOGS"

step "Infrastructure"
make -s infra
LOG="" wait_for "Kafka" 60 port_open 9094
LOG="" wait_for "Schema registry" 60 http_ok http://localhost:8083/apis/ccompat/v7/subjects
LOG="" wait_for "Neo4j" 90 neo4j_ready

step "Collector"
start gossip-collector bash -c "cd gossip-collector && ./gradlew bootRun --no-daemon"
LOG="$LOGS/gossip-collector.log" wait_for "Collector" 180 grep -q "Started .* in .* seconds" "$LOGS/gossip-collector.log"

step "API"
start gossip-api bash -c "cd gossip-api && ./gradlew bootRun --no-daemon"
LOG="$LOGS/gossip-api.log" wait_for "API" 180 http_ok http://localhost:8090/api/v1/topology

step "Agent"
make -s -C gossip-agent build
(cd gossip-agent && sudo ./gossip-agent kafka/kafka-config.json > "$LOGS/gossip-agent.log" 2>&1 &)
sleep 3
pgrep -x gossip-agent > /dev/null || fail "Agent exited" "$LOGS/gossip-agent.log"

step "Streaming service"
make -s streaming
LOG="" wait_for "Streaming service (Eureka registration)" 180 http_ok http://localhost:8080/videos

step "Traffic ($ROUNDS rounds)"
./e2e/traffic.sh "$ROUNDS"
RUN_DIR="$(ls -td e2e/runs/*/ | head -1)"

step "Waiting for the pipeline to drain"
sleep 10

curl -s http://localhost:8090/api/v1/operations | jq . > "$RUN_DIR/operations.json"
docker exec gossip-collector-neo4j-1 cypher-shell -u neo4j -p password --format plain \
    "MATCH (src:Service)-[:SENDS]->(i:Interaction)-[:TO]->(dst:Service)
     RETURN src.ip AS src, dst.ip AS dst, i.dport AS port, i.method AS method, i.path AS path,
            i.count AS count, i.errorCount AS errors
     ORDER BY dst, port, path, method, src" 2> /dev/null | tr -d '"' | tr ', ' '\t' | tr -s '\t' > "$RUN_DIR/actual.tsv"

step "Observed interactions"
column -t -s $'\t' "$RUN_DIR/actual.tsv" >&2

cat >&2 <<EOF

Done. Run saved to $RUN_DIR
  expected.tsv     what the client sent
  actual.tsv       what gossip stored in Neo4j
  operations.json  GET /api/v1/operations

Frontend: make frontend
Stop:     make clear
EOF
