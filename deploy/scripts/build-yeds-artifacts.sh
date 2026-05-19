#!/usr/bin/env bash
# 宿机构建全平台产物（jar + 前端 dist + 登录页静态资源），供 docker compose 挂载
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

export JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 21 2>/dev/null || true)}"

echo "==> yeds-login-web -> iam static"
(cd shared/frontend/yeds-login-web && npm ci && npm run build:deploy)

echo "==> foundation"
(cd foundation && mvn -q -DskipTests install)

echo "==> BIDS backend"
(cd bids/backend && mvn -q -DskipTests package -pl bids-config,bids-exec,bids-export -am)

echo "==> IAM backend"
(cd iam/backend && mvn -q -DskipTests package)

echo "==> ALB controlplane"
(cd alb && mvn -q -DskipTests package -pl alb-controlplane -am)

echo "==> APIG gateway"
(cd apig && mvn -q -DskipTests package -pl gateway-dataplane -am)

echo "==> BIDS frontend"
(cd bids/frontend && npm ci && VITE_API_BASE=/apig npm run build)

echo "==> IAM frontend"
(cd iam/frontend && npm ci && VITE_API_BASE_URL=/apig npm run build)

echo "==> ALB console"
(cd alb/alb-console && npm ci && npm run build)

for j in \
  bids/backend/bids-config/target/bids-config-0.0.1-SNAPSHOT.jar \
  bids/backend/bids-exec/target/bids-exec-0.0.1-SNAPSHOT.jar \
  bids/backend/bids-export/target/bids-export-0.0.1-SNAPSHOT.jar \
  iam/backend/target/iam-backend-0.0.1-SNAPSHOT.jar \
  alb/alb-controlplane/target/alb-controlplane-0.0.1-SNAPSHOT.jar \
  apig/gateway-dataplane/target/gateway-dataplane-1.0.0-SNAPSHOT.jar
do
  test -f "$j" || { echo "missing: $j" >&2; exit 1; }
  size=$(stat -f%z "$j" 2>/dev/null || stat -c%s "$j")
  if [ "$size" -lt 1000000 ]; then
    echo "ERROR: $j 仅 ${size} 字节，疑似未执行 spring-boot repackage，请检查 Maven 与 apig/pom.xml" >&2
    exit 1
  fi
  echo "ok: $j (${size} bytes)"
done

for d in bids/frontend/dist iam/frontend/dist alb/alb-console/dist; do
  test -d "$d" || { echo "missing: $d" >&2; exit 1; }
  echo "ok: $d"
done

echo "==> done"
