# YEDS 全栈单容器镜像（Render Web Service）
# 构建上下文：仓库根目录（见 render.yaml dockerContext: .）

ARG MAVEN_BUILD_IMAGE=maven:3.9.9-eclipse-temurin-21
ARG NODE_IMAGE=node:20-alpine
ARG MYSQL_BASE_IMAGE=mysql:8.0

# --- 登录页静态资源（写入 IAM jar 资源目录）---
FROM ${NODE_IMAGE} AS login-build
WORKDIR /w/shared/frontend/yeds-login-web
COPY shared/frontend/yeds-ui /w/shared/frontend/yeds-ui
COPY shared/frontend/yeds-login-web/package.json shared/frontend/yeds-login-web/package-lock.json ./
RUN npm ci
COPY shared/frontend/yeds-login-web/ ./
RUN npm run build:deploy

# --- Java 后端 ---
FROM ${MAVEN_BUILD_IMAGE} AS backend-build
WORKDIR /w
COPY foundation/pom.xml foundation/pom.xml
COPY foundation/common-web-starter/pom.xml foundation/common-web-starter/pom.xml
COPY foundation/common-web-starter/src foundation/common-web-starter/src
RUN mvn -q -f foundation/pom.xml install -DskipTests

COPY bids/backend/pom.xml bids/backend/pom.xml
COPY bids/backend/bids-config bids/backend/bids-config
COPY bids/backend/bids-exec bids/backend/bids-exec
COPY bids/backend/bids-export bids/backend/bids-export
COPY iam/backend/pom.xml iam/backend/pom.xml
COPY iam/backend/src iam/backend/src
COPY --from=login-build /w/iam/backend/src/main/resources/static/yeds-login \
  iam/backend/src/main/resources/static/yeds-login
COPY alb/pom.xml alb/pom.xml
COPY alb/alb-controlplane alb/alb-controlplane
COPY apig/pom.xml apig/pom.xml
COPY apig/gateway-dataplane apig/gateway-dataplane

RUN mvn -q -f bids/backend/pom.xml package -pl bids-config,bids-exec,bids-export -am -DskipTests \
  && mvn -q -f iam/backend/pom.xml package -DskipTests \
  && mvn -q -f alb/pom.xml package -pl alb-controlplane -am -DskipTests \
  && mvn -q -f apig/pom.xml package -pl gateway-dataplane -am -DskipTests

# --- 前端 ---
FROM ${NODE_IMAGE} AS bids-fe
WORKDIR /w/bids/frontend
COPY shared/frontend/yeds-ui /w/shared/frontend/yeds-ui
COPY shared/frontend/vite-allowed-hosts.js /w/shared/frontend/vite-allowed-hosts.js
COPY bids/frontend/package.json bids/frontend/package-lock.json ./
RUN npm ci
COPY bids/frontend/ ./
ENV VITE_API_BASE=/apig
RUN npm run build

FROM ${NODE_IMAGE} AS iam-fe
WORKDIR /w/iam/frontend
COPY shared/frontend/yeds-ui /w/shared/frontend/yeds-ui
COPY iam/frontend/package.json iam/frontend/package-lock.json ./
RUN npm ci
COPY iam/frontend/ ./
ENV VITE_API_BASE_URL=/apig
RUN npm run build

FROM ${NODE_IMAGE} AS alb-fe
WORKDIR /w/alb/alb-console
COPY shared/frontend/yeds-ui /w/shared/frontend/yeds-ui
COPY shared/frontend/vite-allowed-hosts.js /w/shared/frontend/vite-allowed-hosts.js
COPY alb/alb-console/package.json alb/alb-console/package-lock.json ./
RUN npm ci
COPY alb/alb-console/ ./
RUN npm run build

# --- 运行时：MySQL + JRE + nginx ---
FROM ${MYSQL_BASE_IMAGE}

USER root
RUN apt-get update \
  && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
    openjdk-21-jre-headless nginx gettext-base netcat-openbsd curl \
  && rm -rf /var/lib/apt/lists/* \
  && mkdir -p /opt/yeds/jars /opt/yeds/www/bids /opt/yeds/www/iam /opt/yeds/www/alb \
    /etc/nginx/templates /var/log/yeds

COPY deploy/docker/mysql/init/ /docker-entrypoint-initdb.d/

COPY --from=backend-build /w/bids/backend/bids-config/target/bids-config-0.0.1-SNAPSHOT.jar \
  /opt/yeds/jars/bids-config.jar
COPY --from=backend-build /w/bids/backend/bids-exec/target/bids-exec-0.0.1-SNAPSHOT.jar \
  /opt/yeds/jars/bids-exec.jar
COPY --from=backend-build /w/bids/backend/bids-export/target/bids-export-0.0.1-SNAPSHOT.jar \
  /opt/yeds/jars/bids-export.jar
COPY --from=backend-build /w/iam/backend/target/iam-backend-0.0.1-SNAPSHOT.jar \
  /opt/yeds/jars/iam-backend.jar
COPY --from=backend-build /w/alb/alb-controlplane/target/alb-controlplane-0.0.1-SNAPSHOT.jar \
  /opt/yeds/jars/alb-controlplane.jar
COPY --from=backend-build /w/apig/gateway-dataplane/target/gateway-dataplane-1.0.0-SNAPSHOT.jar \
  /opt/yeds/jars/apig-gateway.jar

COPY --from=bids-fe /w/bids/frontend/dist/ /opt/yeds/www/bids/
COPY --from=iam-fe /w/iam/frontend/dist/ /opt/yeds/www/iam/
COPY --from=alb-fe /w/alb/alb-console/dist/ /opt/yeds/www/alb/

COPY deploy/render/nginx.render.conf.template /etc/nginx/templates/nginx.render.conf.template
COPY deploy/render/start.sh /usr/local/bin/yeds-render-start.sh
RUN chmod +x /usr/local/bin/yeds-render-start.sh \
  && rm -f /etc/nginx/sites-enabled/default /etc/nginx/conf.d/default.conf

ENV MYSQL_DATABASE=bids \
  MYSQL_USER=bids \
  MYSQL_PASSWORD=bids

HEALTHCHECK --interval=30s --timeout=5s --start-period=300s --retries=5 \
  CMD curl -sf "http://127.0.0.1:${PORT:-10000}/bids/" >/dev/null || exit 1

ENTRYPOINT ["/usr/local/bin/yeds-render-start.sh"]
