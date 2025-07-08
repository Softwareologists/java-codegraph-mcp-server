#!/bin/bash
# Example script to run CLI and process a sample jar

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="../cli/samples/example.jar"

java -cp "$JAR" HelloWorld
