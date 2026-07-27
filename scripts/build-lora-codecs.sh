#!/usr/bin/env bash
# Builds the lora-codecs jar from source into libs/.
#
# lora-codecs is no longer resolvable from the retired OSS Sonatype snapshots
# repository, and its Gradle 5.1 build does not run on modern JDKs. The library
# has no third-party runtime dependencies, so compiling it directly is enough.
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
LORA_CODECS_DIR="${LORA_CODECS_DIR:-$(dirname "$PROJECT_DIR")/lora-codecs}"
JAR_NAME="lora-codecs-0.0.1-SNAPSHOT.jar"

if [[ ! -d "$LORA_CODECS_DIR/src/main/java" ]]; then
    echo "lora-codecs sources not found at $LORA_CODECS_DIR (set LORA_CODECS_DIR)" >&2
    exit 1
fi

WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

find "$LORA_CODECS_DIR/src/main/java" -name '*.java' > "$WORK_DIR/sources.txt"
javac --release 11 -d "$WORK_DIR/classes" @"$WORK_DIR/sources.txt"

mkdir -p "$PROJECT_DIR/libs"
jar cf "$PROJECT_DIR/libs/$JAR_NAME" -C "$WORK_DIR/classes" .
echo "Built $PROJECT_DIR/libs/$JAR_NAME"
