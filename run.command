#!/bin/sh
PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$PROJECT_DIR" || exit 1
./run.sh
STATUS=$?
echo
echo "运行结束，退出码：$STATUS；按回车关闭窗口……"
read -r _
exit "$STATUS"

