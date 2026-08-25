#!/bin/sh
set -eu
PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$PROJECT_DIR"
mkdir -p build/test-classes
find src/main/java src/test/java -name '*.java' -print | sort > build/test-sources.txt
javac --release 8 -Xlint:-options -encoding UTF-8 -d build/test-classes @build/test-sources.txt
java -ea -cp build/test-classes com.jundolc.ashare.ScreenerTest
