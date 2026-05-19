#!/usr/bin/env bash
# Render 单容器入口：MySQL + 全栈 Java + 边缘 nginx（监听 $PORT）
set -euo pipefail

export MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-render-root}"
export MYSQL_DATABASE="${MYSQL_DATABASE:-bids}"
export MYSQL_USER="${MYSQL_USER:-bids}"
export MYSQL_PASSWORD="${MYSQL_PASSWORD:-bids}"

export PORT="${PORT:-10000}"
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:--Xms48m -Xmx128m}"

JDBC_BASE="jdbc:mysql://127.0.0.1:3306"
JDBC_SUFFIX="?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"

export IAM_DB_URL="${IAM_DB_URL:-${JDBC_BASE}/iam${JDBC_SUFFIX}}"
export IAM_DB_USERNAME="${IAM_DB_USERNAME:-bids}"
export IAM_DB_PASSWORD="${IAM_DB_PASSWORD:-${MYSQL_PASSWORD}}"
export IAM_SQL_INIT_MODE="${IAM_SQL_INIT_MODE:-never}"

export ALB_DB_URL="${ALB_DB_URL:-${JDBC_BASE}/alb${JDBC_SUFFIX}}"
export ALB_DB_USERNAME="${ALB_DB_USERNAME:-bids}"
export ALB_DB_PASSWORD="${ALB_DB_PASSWORD:-${MYSQL_PASSWORD}}"
export ALB_SQL_INIT_MODE="${ALB_SQL_INIT_MODE:-never}"

export BIDS_CONFIG_DB_URL="${BIDS_CONFIG_DB_URL:-${JDBC_BASE}/bids${JDBC_SUFFIX}}"
export BIDS_CONFIG_DB_USERNAME="${BIDS_CONFIG_DB_USERNAME:-bids}"
export BIDS_CONFIG_DB_PASSWORD="${BIDS_CONFIG_DB_PASSWORD:-${MYSQL_PASSWORD}}"

export BIDS_EXPORT_DB_URL="${BIDS_EXPORT_DB_URL:-${BIDS_CONFIG_DB_URL}}"
export BIDS_EXPORT_DB_USERNAME="${BIDS_EXPORT_DB_USERNAME:-bids}"
export BIDS_EXPORT_DB_PASSWORD="${BIDS_EXPORT_DB_PASSWORD:-${MYSQL_PASSWORD}}"
export BIDS_EXPORT_INTERNAL_TOKEN="${BIDS_EXPORT_INTERNAL_TOKEN:-change-me}"
export BIDS_EXPORT_STORAGE_TYPE="${BIDS_EXPORT_STORAGE_TYPE:-local}"
export BIDS_EXPORT_LOCAL_DIR="${BIDS_EXPORT_LOCAL_DIR:-/tmp/bids-export-files}"

export BIDS_ADMIN_USERNAME="${BIDS_ADMIN_USERNAME:-admin}"
export BIDS_ADMIN_PASSWORD="${BIDS_ADMIN_PASSWORD:-admin}"

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-docker}"
export SPRING_SQL_INIT_MODE="${SPRING_SQL_INIT_MODE:-never}"

export IAM_JWK_SET_URI="${IAM_JWK_SET_URI:-http://127.0.0.1:8091/api/iam/auth/jwks}"
export IAM_AUTHORIZE_CHECK_URL="${IAM_AUTHORIZE_CHECK_URL:-http://127.0.0.1:8091/api/iam/authorize/check}"
export IAM_BASE_URL="${IAM_BASE_URL:-http://127.0.0.1:8091}"
export BIDS_CONFIG_BASE_URL="${BIDS_CONFIG_BASE_URL:-http://127.0.0.1:8081}"
export BIDS_EXEC_BASE_URL="${BIDS_EXEC_BASE_URL:-http://127.0.0.1:8082}"
export BIDS_EXPORT_CLIENT_BASE_URL="${BIDS_EXPORT_CLIENT_BASE_URL:-http://127.0.0.1:8083}"

# bids-exec：关闭 ES 审计，避免依赖 Elasticsearch
export BIDS_AUDIT_ELASTICSEARCH_ENABLED="${BIDS_AUDIT_ELASTICSEARCH_ENABLED:-false}"

JAR_DIR=/opt/yeds/jars
LOG_DIR=/var/log/yeds
mkdir -p "$LOG_DIR" /tmp/bids-export-files

wait_port() {
  local host=$1 port=$2
  local max=${3:-90}
  local i=1
  while [ "$i" -le "$max" ]; do
    if nc -z "$host" "$port" 2>/dev/null; then
      return 0
    fi
    sleep 2
    i=$((i + 1))
  done
  echo "timeout waiting for ${host}:${port}" >&2
  return 1
}

start_java() {
  local name=$1 jar=$2
  shift 2
  echo "==> starting ${name}"
  env "$@" java -jar "$jar" >>"${LOG_DIR}/${name}.log" 2>&1 &
  echo $! >"${LOG_DIR}/${name}.pid"
}

echo "==> starting MySQL"
/docker-entrypoint.sh mysqld &
wait_port 127.0.0.1 3306 120

start_java iam-backend "${JAR_DIR}/iam-backend.jar"
wait_port 127.0.0.1 8091 120

start_java bids-config "${JAR_DIR}/bids-config.jar"
start_java bids-export "${JAR_DIR}/bids-export.jar"
wait_port 127.0.0.1 8081 120
wait_port 127.0.0.1 8083 90

start_java bids-exec "${JAR_DIR}/bids-exec.jar" \
  BIDS_AUDIT_ELASTICSEARCH_ENABLED=false \
  SPRING_AUTOCONFIGURE_EXCLUDE=org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration,org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration,org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration
start_java alb-controlplane "${JAR_DIR}/alb-controlplane.jar"
wait_port 127.0.0.1 8082 120
wait_port 127.0.0.1 8095 90

start_java apig-gateway "${JAR_DIR}/apig-gateway.jar"
wait_port 127.0.0.1 8080 120

echo "==> starting nginx on port ${PORT}"
export PORT
envsubst '${PORT}' < /etc/nginx/templates/nginx.render.conf.template > /etc/nginx/conf.d/default.conf
exec nginx -g 'daemon off;'
