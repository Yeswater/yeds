#!/usr/bin/env bash
# shellcheck disable=SC2034
# 供 compose-up-*.sh source：日志、产物检测、中间件就绪等待（默认 120s）
YEDS_START_TIMEOUT_SEC="${YEDS_START_TIMEOUT_SEC:-120}"
YEDS_DOCKER_CMD_TIMEOUT_SEC="${YEDS_DOCKER_CMD_TIMEOUT_SEC:-30}"

log() {
  printf '[%s] %s\n' "$(date '+%H:%M:%S')" "$*"
}

log_step() {
  printf '\n[%s] >>> %s\n' "$(date '+%H:%M:%S')" "$*"
}

elapsed_since() {
  echo $(( $(date +%s) - $1 ))
}

# 单条 docker 命令超时，避免 Docker Desktop 未就绪时脚本无输出挂死
with_docker_timeout() {
  local secs="${1:-$YEDS_DOCKER_CMD_TIMEOUT_SEC}"
  shift
  if command -v timeout &>/dev/null; then
    timeout "$secs" "$@"
    return $?
  fi
  if command -v gtimeout &>/dev/null; then
    gtimeout "$secs" "$@"
    return $?
  fi
  "$@" &
  local pid=$!
  local i=0
  while kill -0 "$pid" 2>/dev/null; do
    if (( i >= secs )); then
      kill "$pid" 2>/dev/null || true
      wait "$pid" 2>/dev/null || true
      return 124
    fi
    sleep 1
    ((i++)) || true
  done
  wait "$pid"
}

ensure_docker_ready() {
  log_step "检查 Docker 引擎"
  if ! with_docker_timeout 15 docker info &>/dev/null; then
    log "Docker 未响应（${YEDS_DOCKER_CMD_TIMEOUT_SEC}s 内无应答）。请启动 Docker Desktop 后重试。"
    return 1
  fi
  log "Docker 引擎正常"
  return 0
}

# 全栈运行时所需 jar / dist 是否齐全
yeds_artifacts_ready() {
  local root="$1"
  local f
  for f in \
    "$root/bids/backend/bids-config/target/bids-config-0.0.1-SNAPSHOT.jar" \
    "$root/bids/backend/bids-exec/target/bids-exec-0.0.1-SNAPSHOT.jar" \
    "$root/bids/backend/bids-export/target/bids-export-0.0.1-SNAPSHOT.jar" \
    "$root/iam/backend/target/iam-backend-0.0.1-SNAPSHOT.jar" \
    "$root/alb/alb-controlplane/target/alb-controlplane-0.0.1-SNAPSHOT.jar" \
    "$root/apig/gateway-dataplane/target/gateway-dataplane-1.0.0-SNAPSHOT.jar" \
    "$root/bids/frontend/dist/index.html" \
    "$root/iam/frontend/dist/index.html" \
    "$root/alb/alb-console/dist/index.html"
  do
    [[ -f "$f" ]] || return 1
  done
  return 0
}

# 等待中间件健康（带进度，超时秒数）
wait_middleware_healthy() {
  local compose_cmd=("$@")
  local timeout="${YEDS_START_TIMEOUT_SEC}"
  local start
  start=$(date +%s)
  local services=(mysql postgres elasticsearch rustfs)
  log "等待中间件就绪（超时 ${timeout}s）：${services[*]}"

  while true; do
    local el
    el=$(elapsed_since "$start")
    if (( el >= timeout )); then
      log "超时：中间件未在 ${timeout}s 内全部 healthy"
      docker compose "${compose_cmd[@]}" ps "${services[@]}" 2>/dev/null || true
      return 1
    fi

    local all_ok=1
    local status_line=""
    for svc in "${services[@]}"; do
      local cid health
      cid=$(docker compose "${compose_cmd[@]}" ps -q "$svc" 2>/dev/null | head -1)
      if [[ -z "$cid" ]]; then
        all_ok=0
        status_line+="$svc:未创建 "
        continue
      fi
      health=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$cid" 2>/dev/null || echo "unknown")
      status_line+="$svc:$health "
      if [[ "$svc" == "rustfs" ]]; then
        [[ "$health" == "running" ]] || all_ok=0
      elif [[ "$health" != "healthy" ]]; then
        all_ok=0
      fi
    done

    log "  ${el}s | ${status_line}"
    if (( all_ok )); then
      log "中间件已就绪（${el}s）"
      return 0
    fi
    sleep 5
  done
}

# 执行 profile build（有超时与日志）；失败返回 1
run_profile_build() {
  local compose_cmd=("$@")
  local timeout="${YEDS_START_TIMEOUT_SEC}"
  local start
  start=$(date +%s)

  log_step "容器内构建 jar / 前端（profile build，超时 ${timeout}s）"
  log "首次或代码变更后可能超过 2 分钟，每 10s 输出进度…"

  set +e
  (
    docker compose "${compose_cmd[@]}" --profile build up --abort-on-container-exit 2>&1
  ) &
  local up_pid=$!
  set -e

  while kill -0 "$up_pid" 2>/dev/null; do
    local el
    el=$(elapsed_since "$start")
    if (( el >= timeout )); then
      log "构建超时（${timeout}s），终止…"
      kill "$up_pid" 2>/dev/null || true
      docker compose "${compose_cmd[@]}" --profile build down --remove-orphans 2>/dev/null || true
      return 1
    fi
    docker compose "${compose_cmd[@]}" --profile build ps 2>/dev/null | tail -6 | while IFS= read -r line; do
      [[ -n "$line" ]] && log "  $line"
    done
    log "  构建进行中… ${el}s / ${timeout}s"
    sleep 10
  done
  wait "$up_pid"
  local rc=$?

  if (( rc != 0 )); then
    log "构建失败（exit $rc），最近日志："
    docker compose "${compose_cmd[@]}" --profile build logs --tail=50 2>/dev/null || true
    return 1
  fi

  log "构建完成（$(elapsed_since "$start")s）"
  return 0
}
