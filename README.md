# YEDS

**YEDS**（Yeswater Enterprise Digital System）是 Yeswater 企业数字化系统 monorepo，包含 BIDS、IAM、APIG、ALB 等平台组件。

## 快速开始（Docker）

在**仓库根目录**执行（Compose 项目名 `yeds`）。先复制环境变量：`cp .env.example .env`。

### 一键启动全栈

含中间件、自动构建 jar/前端、BIDS/IAM/APIG/ALB 业务容器与统一入口 nginx。

```bash
# macOS（Linux/Windows 见 env/构建环境-全栈Docker模式.md）
docker compose -f docker-compose.yml -f docker-compose.macos.yml build --pull=false mysql   # 仅首次
docker compose -f docker-compose.yml -f docker-compose.macos.yml --profile full up -d
```

或使用脚本（带进度日志，产物已存在时目标 2 分钟内）：

```bash
./deploy/scripts/compose-up-full.sh --build-mysql   # 首次加 --build-mysql
./deploy/scripts/compose-up-full.sh
```

**不要**使用 `docker compose up -d --build` 拉全量业务镜像；业务服务挂载宿主机 `target/*.jar`，仅 `mysql` 需要 `build`。

全栈前请执行 `./deploy/scripts/build-yeds-artifacts.sh`（或 `docker compose --profile build` 构建 jar/前端），确保 `iam-backend`、`gateway-dataplane` 等为可执行的 Spring Boot fat jar（约数十 MB）。若 API 返回 404，检查 ALB 是否已挂载 `deploy/docker/alb/api-upstream-locations.conf`；若 `/apig/**` 502/超时，检查 `apig/gateway-dataplane/target/*.jar` 是否已 repackage。

### 仅启动第三方中间件（推荐日常开发）

MySQL、PostgreSQL、Elasticsearch、RustFS；业务在宿主机 `mvn spring-boot:run` / `npm run dev`。

```bash
docker compose -f docker-compose.yml -f docker-compose.macos.yml up -d
# 等价于只起 mysql postgres elasticsearch rustfs（无 profile 的服务）
```

或（带时间戳与就绪等待，默认 120s 内完成）：

```bash
./deploy/scripts/compose-up-middleware.sh
```

### 常用入口（全栈）

| 入口 | URL |
|------|-----|
| ALB 统一入口 | http://127.0.0.1/ |
| BIDS 控制台 | http://127.0.0.1:8080/bids/ |
| IAM 控制台 | http://127.0.0.1:5181/iam/ |
| ALB 路由管理 | http://127.0.0.1:5185/alb/ |

## 文档

| 文档 | 说明 |
|------|------|
| [env/构建环境.md](env/构建环境.md) | 构建模式总览 |
| [env/构建环境-本机开发模式.md](env/构建环境-本机开发模式.md) | 中间件 Docker + 本机业务 |
| [env/构建环境-全栈Docker模式.md](env/构建环境-全栈Docker模式.md) | 全栈容器联调 |
| [docs/ALB路由与部署指南.md](docs/ALB路由与部署指南.md) | 边缘路由与文根 |
| [.cursor/rules/yeds-docker-compose.mdc](.cursor/rules/yeds-docker-compose.mdc) | Agent Docker 约定 |

## 组件目录

| 目录 | 说明 |
|------|------|
| `bids/` | 数据服务（配置、执行、导出） |
| `iam/` | 身份与访问管理 |
| `apig/` | API 网关 |
| `alb/` | 边缘路由（nginx + 控制面） |
| `foundation/` | 公共 `common-web-starter` 等 |
| `shared/frontend/` | 统一登录与 UI 库 |
| `deploy/docker/` | MySQL init、nginx 等 Docker 资产 |

## 本机构建产物（可选）

全栈 `up` 会自动在容器内构建；也可在宿主机预构建后启动：

```bash
./deploy/scripts/build-yeds-artifacts.sh
```

本地 Java 开发前须：`cd foundation && mvn -DskipTests install`。
