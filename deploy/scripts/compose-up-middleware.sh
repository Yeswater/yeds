#!/usr/bin/env bash
# 仅启动第三方中间件：MySQL、PostgreSQL、Elasticsearch、RustFS（目标 <2min，带进度日志）
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
# shellcheck source=compose-files.sh
source "$ROOT/deploy/scripts/compose-files.sh"
# shellcheck source=compose-lib.sh
source "$ROOT/deploy/scripts/compose-lib.sh"
cd "$ROOT"

START_TS=$(date +%s)
COMPOSE=(docker compose "${COMPOSE_FILES[@]}")

ensure_docker_ready || exit 1

if [[ "${1:-}" == "--build-mysql" ]]; then
  shift
  log_step "构建 MySQL 镜像（含 init 脚本）"
  "${COMPOSE[@]}" build --progress=plain --pull=false mysql 2>&1 | while IFS= read -r line; do log "  $line"; done
fi

log_step "检查 MySQL 镜像 yeds-mysql:8"
if ! with_docker_timeout 10 docker image inspect yeds-mysql:8 &>/dev/null; then
  log "未找到 yeds-mysql:8，开始构建（首次约 1–3 分钟）…"
  "${COMPOSE[@]}" build --progress=plain --pull=false mysql 2>&1 | while IFS= read -r line; do log "  $line"; done
fi

log_step "拉取中间件镜像（postgres / elasticsearch / rustfs）"
with_docker_timeout 90 "${COMPOSE[@]}" pull postgres elasticsearch rustfs 2>&1 | while IFS= read -r line; do
  log "  $line"
done || log "  pull 部分镜像跳过或超时，继续 up…"

log_step "创建并启动容器"
with_docker_timeout 60 "${COMPOSE[@]}" up -d mysql postgres elasticsearch rustfs "$@" 2>&1 | while IFS= read -r line; do
  log "  $line"
done

wait_middleware_healthy "${COMPOSE_FILES[@]}" || exit 1

log_step "中间件状态"
"${COMPOSE[@]}" ps mysql postgres elasticsearch rustfs

EL=$(elapsed_since "$START_TS")
log "完成：中间件启动总耗时 ${EL}s（目标 ≤${YEDS_START_TIMEOUT_SEC}s）"
if (( EL > YEDS_START_TIMEOUT_SEC )); then
  log "提示：超过 ${YEDS_START_TIMEOUT_SEC}s，多为首次拉镜像/ES 冷启动；再次执行通常更快。"
  exit 1
fi
