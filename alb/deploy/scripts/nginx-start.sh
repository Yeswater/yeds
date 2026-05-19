#!/usr/bin/env bash
# 本机 nginx（未安装 Docker 时）：需已安装 nginx，且监听 80 可能需要 sudo
set -euo pipefail
DIR="$(cd "$(dirname "$0")/.." && pwd)"
CONF="$DIR/nginx/nginx.dev.conf"
RUN_DIR="$DIR/nginx/run"
mkdir -p "$RUN_DIR"

if ! command -v nginx >/dev/null 2>&1; then
  echo "未找到 nginx，请使用: cd $DIR && docker compose up -d"
  exit 1
fi

nginx -t -c "$CONF" -g "pid $RUN_DIR/nginx.pid; error_log $RUN_DIR/error.log;" 2>/dev/null || {
  nginx -t -c /dev/null 2>/dev/null || true
  echo "使用包装配置启动..."
  exec nginx -p "$RUN_DIR" -c "$CONF"
}

echo "推荐: cd $DIR && docker compose up -d"
