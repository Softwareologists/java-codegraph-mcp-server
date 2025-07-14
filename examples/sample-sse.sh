#!/bin/bash
# Builds the CLI fat-JAR and runs a sample query against example.jar using SSE
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# Build CLI fat JAR
cd "$ROOT_DIR"
gradle -q :cli:shadowJar
CLI_JAR="$ROOT_DIR/cli/build/libs/cli-all.jar"

# Prepare watch directory with provided example JAR
WATCH_DIR="$(mktemp -d)"
EXAMPLE_JAR="$SCRIPT_DIR/example.jar"
if [[ ! -f "$EXAMPLE_JAR" ]]; then
  echo "Missing $EXAMPLE_JAR. Run build-example-jar.sh first." >&2
  exit 1
fi
cp "$EXAMPLE_JAR" "$WATCH_DIR/"

PORT=8080
java -jar "$CLI_JAR" --watch-dir "$WATCH_DIR" --sse-port "$PORT" &
PID=$!
sleep 1
curl -X POST -H "Content-Type: application/json" \
  -d '{"findCallers":"dep.A"}' \
  http://localhost:$PORT/mcp/query
kill $PID
rm -rf "$WATCH_DIR"
