#!/usr/bin/env bash
# 输出本机推荐的 compose -f 参数（供其他脚本 source）
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
COMPOSE_FILES=(-f "$ROOT/docker-compose.yml")
case "$(uname -s)" in
  Darwin) COMPOSE_FILES+=(-f "$ROOT/docker-compose.macos.yml") ;;
  Linux) COMPOSE_FILES+=(-f "$ROOT/docker-compose.linux.yml") ;;
  MINGW*|MSYS*|CYGWIN*) COMPOSE_FILES+=(-f "$ROOT/docker-compose.windows.yml") ;;
esac
