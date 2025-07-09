#!/bin/bash
# Builds a small sample JAR used by sample-cli.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
WORK_DIR="$(mktemp -d)"
mkdir -p "$WORK_DIR/dep"

cat > "$WORK_DIR/dep/A.java" <<EOM
package dep; public class A {}
EOM
cat > "$WORK_DIR/dep/B.java" <<EOM
package dep; public class B { A a; }
EOM

javac -d "$WORK_DIR" "$WORK_DIR/dep/A.java" "$WORK_DIR/dep/B.java"
jar cf "$SCRIPT_DIR/example.jar" -C "$WORK_DIR" dep/A.class -C "$WORK_DIR" dep/B.class

rm -rf "$WORK_DIR"

echo "Created $SCRIPT_DIR/example.jar"
