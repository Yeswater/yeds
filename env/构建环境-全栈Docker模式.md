# YEDS 构建环境（全栈 Docker 模式）

适用场景：联调、演示、快速拉起完整环境、降低本机工具链依赖。

---

## 1. 前置条件

- Docker 20.10+
- Docker Compose v2

检查：

```bash
docker version
docker compose version
```

---

## 2. 端口基线

| 服务 | 端口 |
|---|---|
| bids-web | 8080 |
| bids-config | 8081 |
| bids-exec | 8082 |
| bids-export（现状） | 8083 |
| MySQL | 3306 |
| PostgreSQL | 5432 |
| Elasticsearch | 9200 |
| RustFS | 9000 / 9001 |

---

## 3. 启动方式

```bash
cd bids
cp .env.example .env

# macOS
docker compose -f docker-compose.yml -f docker-compose.macos.yml build --pull=false
docker compose -f docker-compose.yml -f docker-compose.macos.yml up -d

# Linux
docker compose -f docker-compose.yml -f docker-compose.linux.yml build --pull=false
docker compose -f docker-compose.yml -f docker-compose.linux.yml up -d

# Windows
docker compose -f docker-compose.yml -f docker-compose.windows.yml build --pull=false
docker compose -f docker-compose.yml -f docker-compose.windows.yml up -d
```

查看状态：

```bash
docker compose ps
docker compose logs -f bids-config bids-exec bids-export bids-web
```

---

## 4. 访问入口

- Web 控制台：`http://127.0.0.1:8080`
- 配置接口：`http://127.0.0.1:8081/api/config`
- 运行接口：`http://127.0.0.1:8082/api/runtime`
- 导出接口（内部）：`http://127.0.0.1:8083/api/export/v1`

---

## 5. 常用命令

只重建应用层：

```bash
cd bids
./scripts/compose-up-web.sh
```

仅启动中间件：

```bash
cd bids
docker compose up -d mysql postgres elasticsearch rustfs
```

---

## 6. 常见问题

### 6.1 拉镜像慢/失败

- 在 `bids/.env` 配置 `REGISTRY_PREFIX`。
- 使用 `docker compose build --pull=false`。

### 6.2 端口冲突

```bash
lsof -i :8080 -i :8081 -i :8082 -i :8083 -i :3306 -i :5432 -i :9200 -i :9000 -i :9001
```

---

## 7. 说明

- 当前导出实现仍包含 `bids-export`，后续按设计迁移至 `export-center`。
- `iam`、`edm`、`export-center` 为设计/规划态，代码模块落地后补充容器编排。
