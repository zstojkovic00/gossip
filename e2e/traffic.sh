#!/usr/bin/env bash
# Usage: ./traffic.sh [rounds]
# Sends a fixed scenario through the API Gateway and records every response,
# so that the result can be compared with what gossip stored in Neo4j.

set -u

BASE_URL="${BASE_URL:-http://localhost:8080}"
ROUNDS="${1:-1}"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
VIDEO_FILE="$SCRIPT_DIR/../streaming-service/video-service/http/assets/marvel-spiderman.mp4"
RUN_ID="$(date +%Y%m%d-%H%M%S)"
RUN_DIR="$SCRIPT_DIR/runs/$RUN_ID"
mkdir -p "$RUN_DIR"

REQUESTS="$RUN_DIR/requests.tsv"
BODY="$RUN_DIR/last-body"
printf 'time\tmethod\tpath\tstatus\n' > "$REQUESTS"

call() {
    local method="$1" path="$2"
    shift 2
    local status
    status=$(curl -s -o "$BODY" -w '%{http_code}' -X "$method" "$BASE_URL$path" "$@")
    printf '%s\t%s\t%s\t%s\n' "$(date +%H:%M:%S.%3N)" "$method" "$path" "$status" >> "$REQUESTS"
    printf '%-6s %-45s %s\n' "$method" "$path" "$status" >&2
    echo "$status"
}

{
    echo "run_id=$RUN_ID"
    echo "base_url=$BASE_URL"
    echo "rounds=$ROUNDS"
    echo "start=$(date --iso-8601=seconds)"
} > "$RUN_DIR/meta.txt"

for round in $(seq 1 "$ROUNDS"); do
    echo "--- round $round/$ROUNDS" >&2

    email="e2e-$RUN_ID-$round@gossip.test"
    credentials=$(printf '{"username":"e2e-%s","email":"%s","password":"secret123","role":"USER"}' "$round" "$email")

    call POST /auth/register -H 'Content-Type: application/json' -d "$credentials" > /dev/null
    call POST /auth/login -H 'Content-Type: application/json' \
        -d "$(printf '{"email":"%s","password":"secret123"}' "$email")" > /dev/null
    token=$(jq -r '.token // empty' "$BODY" 2>/dev/null)

    if [ -n "$token" ]; then
        call GET /users/principal -H "Authorization: Bearer $token" > /dev/null
        # upload makes video-service write to MongoDB and publish an event to Kafka
        call POST /videos -H "Authorization: Bearer $token" \
            -F "description=e2e upload $round" -F "genre=E2E" \
            -F "file=@$VIDEO_FILE;type=video/mp4" > /dev/null
    else
        echo "    no token from login, skipping /users/principal and upload" >&2
    fi

    call GET /videos > /dev/null
    video_id=$(jq -r '.[0].id // empty' "$BODY" 2>/dev/null)

    if [ -n "$video_id" ]; then
        call GET "/videos/$video_id" > /dev/null
        call GET "/videos/preview/$video_id" > /dev/null
        call GET "/videos/stream/$video_id" -H 'Range: bytes=0-1048575' > /dev/null
    else
        echo "    no video in /videos, skipping video calls" >&2
    fi

    call GET /videos/does-not-exist > /dev/null
done

echo "end=$(date --iso-8601=seconds)" >> "$RUN_DIR/meta.txt"
rm -f "$BODY"

{
    printf 'method\tpath\tstatus\tcount\n'
    awk -F'\t' 'NR > 1 { count[$2 "\t" $3 "\t" $4]++ }
        END { for (k in count) print k "\t" count[k] }' "$REQUESTS" | sort
} > "$RUN_DIR/expected.tsv"

echo >&2
echo "Run saved to $RUN_DIR" >&2
column -t -s $'\t' "$RUN_DIR/expected.tsv" >&2
