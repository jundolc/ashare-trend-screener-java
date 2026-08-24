#!/bin/sh
set -eu
PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$PROJECT_DIR"
mkdir -p build/classes output
find src/main/java -name '*.java' -print | sort > build/sources.txt
javac --release 8 -encoding UTF-8 -d build/classes @build/sources.txt
exec java -cp build/classes com.jundolc.ashare.Main "$@"
