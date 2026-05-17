# IAM 组件

本目录承载 YEDS 平台的 IAM（Identity and Access Management）实现。

## 当前结构

- `docs/`：设计、实施与检查清单文档。
- `backend/`：IAM MVP 后端服务（Spring Boot + H2）。

## 快速启动

```bash
cd iam/backend
mvn spring-boot:run
```

默认端口 `8091`，示例账号：

- username: `admin`
- password: `admin123`

## 已实现接口（MVP）

```text
POST /api/iam/auth/login
POST /api/iam/auth/refresh
POST /api/iam/auth/logout
GET  /api/iam/auth/jwks

POST /api/iam/authorize/check
POST /api/iam/authorize/batch-check
GET  /api/iam/authorize/user-permissions

POST /api/iam/users
POST /api/iam/roles
POST /api/iam/roles/{roleId}/permissions
POST /api/iam/policies
POST /api/iam/clients
POST /api/iam/clients/{clientId}/rotate-secret
```
