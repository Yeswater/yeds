#!/usr/bin/env bash
# 构建产物并启动后端 + 网关 + IAM + ALB 控制面（compose 内自动构建）
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

COMPOSE=(docker compose -f docker-compose.yml)
if [[ -f docker-compose.macos.yml ]] && [[ "$(uname -s)" == Darwin ]]; then
  COMPOSE+=(-f docker-compose.macos.yml)
fi

"${COMPOSE[@]}" --profile full up -d \
  iam-backend alb-controlplane apig-gateway \
  bids-config bids-exec bids-export \
  "$@"
