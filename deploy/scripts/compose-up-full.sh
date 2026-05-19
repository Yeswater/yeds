#!/usr/bin/env bash
# 全栈启动：中间件 + 业务 + ALB（产物已存在时目标 <2min；缺产物时先 profile build）
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

if [[ ! -f .env ]] && [[ -f .env.example ]]; then
  cp .env.example .env
  log "已复制 .env.example -> .env"
fi

if [[ "${1:-}" == "--build-mysql" ]]; then
  shift
  log_step "构建 MySQL 镜像"
  "${COMPOSE[@]}" build --progress=plain --pull=false mysql 2>&1 | while IFS= read -r line; do log "  $line"; done
fi

log_step "检查 MySQL 镜像"
if ! with_docker_timeout 10 docker image inspect yeds-mysql:8 &>/dev/null; then
  log "构建 yeds-mysql:8…"
  "${COMPOSE[@]}" build --progress=plain --pull=false mysql 2>&1 | while IFS= read -r line; do log "  $line"; done
fi

if yeds_artifacts_ready "$ROOT"; then
  log "本地 jar/dist 已就绪，跳过 profile build（快速路径）"
else
  log "缺少产物，执行 profile build（可能 >2min，属首次/代码变更）"
  export YEDS_START_TIMEOUT_SEC=900
  run_profile_build "${COMPOSE_FILES[@]}" || {
    log "失败时可执行: ./deploy/scripts/build-yeds-artifacts.sh"
    exit 1
  }
  export YEDS_START_TIMEOUT_SEC=120
fi

log_step "启动中间件"
with_docker_timeout 90 "${COMPOSE[@]}" pull postgres elasticsearch rustfs 2>&1 | while IFS= read -r line; do log "  $line"; done || true
with_docker_timeout 60 "${COMPOSE[@]}" up -d mysql postgres elasticsearch rustfs 2>&1 | while IFS= read -r line; do log "  $line"; done

wait_middleware_healthy "${COMPOSE_FILES[@]}" || exit 1

log_step "启动全栈业务（profile full）"
with_docker_timeout 90 "${COMPOSE[@]}" --profile full up -d 2>&1 | while IFS= read -r line; do
  log "  $line"
done

log_step "服务状态"
"${COMPOSE[@]}" --profile full ps

EL=$(elapsed_since "$START_TS")
log "完成：全栈启动总耗时 ${EL}s"
log "入口: http://127.0.0.1/  |  BIDS http://127.0.0.1:8080/bids/"
if yeds_artifacts_ready "$ROOT" && (( EL > YEDS_START_TIMEOUT_SEC )); then
  log "提示：产物已就绪但仍超过 ${YEDS_START_TIMEOUT_SEC}s，检查 ES/MySQL 健康等待。"
  exit 1
fi
