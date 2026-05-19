#!/usr/bin/env bash
# 构建 BIDS 前端 dist（compose 已集成 bids-frontend-build，本脚本供宿主机单独构建）
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT/bids/frontend"
export VITE_API_BASE="${VITE_API_BASE:-/apig}"
if [[ -f package-lock.json ]]; then
  npm ci
else
  npm install
fi
npm run build
test -d dist || { echo "missing dist/" >&2; exit 1; }
echo "ok: bids/frontend/dist"
