#!/usr/bin/env sh
set -e
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
docker compose -f docker-compose.yml -f docker-compose.macos.yml up -d \
  bids-frontend-build bids-web alb-nginx
