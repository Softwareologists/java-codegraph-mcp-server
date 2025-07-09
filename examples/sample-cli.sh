#!/bin/bash
# Builds the CLI fat-JAR and runs a sample query against example.jar
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

# Run CLI with the generated jar and send a sample query
JAVA_OPTS="-Dserver.config.strict_validation.enabled=false"
java $JAVA_OPTS -jar "$CLI_JAR" --watch-dir "$WATCH_DIR" --stdio <<EOF_QUERY
{"findCallers":"dep.A"}
EOF_QUERY

# Clean up temporary directory
rm -rf "$WATCH_DIR"
