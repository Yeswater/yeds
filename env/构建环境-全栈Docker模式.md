# YEDS 构建环境（全栈 Docker 模式）

适用场景：联调、演示、一键拉起完整环境。  
业务后端挂载宿主机 `target/*.jar`；`up` 时自动执行 compose 内 `*-build` 任务，**无需**手动 `build-bids-jars.sh`。

---

## 1. 一键启动（推荐）

在**仓库根目录**：

```bash
cp .env.example .env

# 首次：构建带 init 脚本的 MySQL 镜像
docker compose -f docker-compose.yml -f docker-compose.macos.yml build --pull=false mysql

# 全栈（profile full = 构建任务 + 全部业务 + ALB）
docker compose -f docker-compose.yml -f docker-compose.macos.yml --profile full up -d
```

脚本等价（**带时间戳日志**，不会长时间无输出；本地 jar/dist 已存在时目标 **≤2 分钟**）：

```bash
./deploy/scripts/compose-up-full.sh --build-mysql   # 首次
./deploy/scripts/compose-up-full.sh
```

缺产物时会先跑 `profile build`（可能 >2min，脚本每 10s 打印进度）。

### 注意

| 做法 | 说明 |
|------|------|
| ✅ `--profile full up -d` | 启动全栈 |
| ✅ `build --pull=false mysql` | 仅 MySQL 需要镜像构建 |
| ❌ `up -d --build` | 勿用；易误解为重建所有服务 |
| ❌ `cd bids` 再 compose | 已废弃，须在仓库根 |

---

## 2. 仅中间件（与全栈对比）

日常开发请用**本机开发模式**，只起第三方组件：

```bash
docker compose -f docker-compose.yml -f docker-compose.macos.yml up -d
# 或 ./deploy/scripts/compose-up-middleware.sh
```

不加 `--profile full` 时**不会**启动 BIDS/IAM/APIG/ALB 业务容器。

---

## 3. 端口与访问

| 服务 | 端口 |
|------|------|
| ALB 统一入口 | 80 |
| bids-web | 8080 |
| iam-web | 5181 |
| alb-console-web | 5185 |
| bids-config / exec / export | 8081 / 8082 / 8083 |
| iam-backend / alb-controlplane | 8091 / 8095 |
| apig-gateway | 仅容器网络 8080 |
| MySQL / PostgreSQL / ES / RustFS | 3306 / 5432 / 9200 / 9000·9001 |

| 入口 | URL |
|------|-----|
| ALB | http://127.0.0.1/ |
| BIDS | http://127.0.0.1:8080/bids/ |
| IAM | http://127.0.0.1:5181/iam/ |
| ALB 控制台 | http://127.0.0.1:5185/alb/ |

---

## 4. 查看状态与日志

```bash
docker compose -f docker-compose.yml -f docker-compose.macos.yml --profile full ps
docker compose -f docker-compose.yml -f docker-compose.macos.yml logs -f yeds-backend-build
docker compose -f docker-compose.yml -f docker-compose.macos.yml logs -f yeds-bids-config
```

---

## 5. 改代码后

```bash
# 仅重建 jar
docker compose -f docker-compose.yml -f docker-compose.macos.yml run --rm yeds-backend-build
docker compose -f docker-compose.yml -f docker-compose.macos.yml --profile full restart bids-config bids-exec bids-export iam-backend apig-gateway

# 或宿主机
./deploy/scripts/build-yeds-artifacts.sh
docker compose ... --profile full restart <服务名>
```

---

## 6. 常见问题

### 6.1 找不到 app.jar

`docker compose logs yeds-backend-build` 或执行 `./deploy/scripts/build-yeds-artifacts.sh`。

### 6.2 No such image: yeds-mysql:8

`docker compose build --pull=false mysql`。

### 6.3 项目名/卷迁移

旧项目名 `bids` → `yeds`；清库：`docker compose down -v`。

### 6.4 端口冲突

`lsof -i :80 -i :8080 -i :8081 -i :3306`

---

## 7. 说明

- Docker 资产：`deploy/docker/`
- `apig/deploy/docker-compose.yml` 为 APIG 独立演示栈，不与 YEDS 根 compose 混用
