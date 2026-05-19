# YEDS ALB（边缘路由）

平台文档（部署、转发策略、路由管理）：**[docs/ALB路由与部署指南.md](../docs/ALB路由与部署指南.md)**

## 快速命令

```bash
./deploy/scripts/install-hosts.sh
cd deploy && docker compose up -d
```

访问：**http://localhost/iam/** | **http://localhost/bids/** | **http://localhost/apig/iam/login/**

## 模块

| 目录 | 说明 |
|------|------|
| `deploy/nginx/` | **数据面**（nginx，当前生效） |
| `alb-controlplane/` | 路由 CRUD、发布（:8095） |
| `alb-console/` | 管理 UI（:5185，经 `/alb/`） |
| `alb-dataplane/` | 已废弃 |

## 本目录文档

- [路由服务设计文档](docs/路由服务设计文档.md) — 设计备忘
- [本地开发指南](docs/本地开发指南.md) — 指向平台文档
