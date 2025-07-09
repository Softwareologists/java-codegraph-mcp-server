#!/bin/bash
# Builds the CLI and a temporary sample JAR, then runs a query via STDIN
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# Build CLI fat JAR
cd "$ROOT_DIR"
gradle -q :cli:shadowJar
CLI_JAR="$ROOT_DIR/cli/build/libs/cli-all.jar"

# Prepare watch directory with sample jar
WATCH_DIR="$(mktemp -d)"
SRC_DIR="$WATCH_DIR/src"
mkdir -p "$SRC_DIR/dep"

cat > "$SRC_DIR/dep/A.java" <<EOM
package dep; public class A {}
EOM
cat > "$SRC_DIR/dep/B.java" <<EOM
package dep; public class B { A a; }
EOM

javac -d "$SRC_DIR" "$SRC_DIR/dep/A.java" "$SRC_DIR/dep/B.java"
jar cf "$WATCH_DIR/callers.jar" -C "$SRC_DIR" dep/A.class -C "$SRC_DIR" dep/B.class

# Run CLI with the generated jar and send a sample query
JAVA_OPTS="-Dserver.config.strict_validation.enabled=false"
java $JAVA_OPTS -jar "$CLI_JAR" --watch-dir "$WATCH_DIR" --stdio <<EOF_QUERY
{"findCallers":"dep.A"}
EOF_QUERY

# Clean up temporary directory
rm -rf "$WATCH_DIR"
