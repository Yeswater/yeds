# BIDS 接入 yeds 安全加固联调说明

## 1. 改造目标

- 保留现有 `IAM /iam/login + callback` 登录协议，不升级 OIDC 授权码流。
- BIDS 前端统一走 yeds 网关入口，避免开发态直连 IAM 端口。
- 收敛 token 暴露面：回调落盘后立即清理地址栏敏感参数。
- 打通并默认启用 `网关 -> BIDS` 的 trusted-header 身份链。
- 修复租户透传：IAM access token 补充 `tenant_code`，网关透传到 `X-Bids-Tenant`。
- 收紧 IAM 暴露面：`/api/iam/authorize/**` 与 `/api/iam/manage/**` 增加内部访问令牌校验。

## 2. 关键配置项

### 2.1 前端（`bids/frontend`）

- `VITE_YEDS_LOGIN_URL`：yeds 平台登录页地址，推荐 `http://127.0.0.1:8080/iam/login`。
- `VITE_IAM_LOGIN_URL`：兼容旧变量，未配置 `VITE_YEDS_LOGIN_URL` 时作为回退。
- `VITE_BIDS_APP_URL`：平台切换中 BIDS 地址（可选）。
- `VITE_IAM_APP_URL`：平台切换中 IAM 地址（可选）。
- `VITE_EDM_APP_URL`：平台切换中 EDM 地址（可选）。

### 2.2 网关（`apig/gateway-dataplane`）

- `BIDS_TRUSTED_HEADER_TOKEN`：下游 trusted-header 令牌，默认 `yeds-bids-trusted-v1`。
- `IAM_INTERNAL_ACCESS_TOKEN`：访问 IAM 授权/管理接口的内部令牌，默认 `yeds-iam-internal-v1`。
- `IAM_BASE_URL`：IAM 后端地址，默认 `http://127.0.0.1:8091`。

### 2.3 BIDS 后端（`bids-config` / `bids-exec`）

- `BIDS_TRUSTED_HEADER_ENABLED`：默认 `true`，开启 trusted-header 模式。
- `BIDS_TRUSTED_HEADER_TOKEN`：必须与网关 `BIDS_TRUSTED_HEADER_TOKEN` 一致。

### 2.4 IAM 后端（`iam/backend`）

- `IAM_INTERNAL_ACCESS_ENABLED`：默认 `true`，开启内部访问控制。
- `IAM_INTERNAL_ACCESS_HEADER`：默认 `X-Iam-Internal-Token`。
- `IAM_INTERNAL_ACCESS_TOKEN`：必须与网关 `IAM_INTERNAL_ACCESS_TOKEN` 一致。

## 3. 启动顺序（本地联调）

1. 启动 IAM 后端（`8091`）。
2. 启动 BIDS 后端：`bids-config(8081)`、`bids-exec(8082)`。
3. 启动 yeds 网关 `gateway-dataplane(8080)`。
4. 启动 BIDS 前端 `vite(5173)`。

> 本地开发建议仍按仓库约定：中间件用 Docker，自研 Java/Vue 服务优先宿主机运行。

## 4. 主链路验收

1. 访问 BIDS 前端受保护页面，自动跳转 `/iam/login`。
2. 登录成功后回到 `/auth/iam/callback`，随后跳转到业务路由（默认 `/run/svc`）。
3. 在浏览器地址栏确认 `access_token`、`refresh_token`、`expires_in` 已被清理。
4. 调用 `/api/runtime/**` 与 `/api/config/**` 成功，后端能识别当前用户。
5. 模拟 token 过期后再次请求，触发 refresh 并继续访问。
6. 退出登录后回到登录入口，受保护页面不可直接访问。
7. 使用不同租户用户登录，检查授权结果与租户隔离是否符合预期。

## 5. 常见故障排查

- **现象：BIDS 后端全部 401/403**
  - 检查 `BIDS_TRUSTED_HEADER_ENABLED=true`。
  - 检查 `BIDS_TRUSTED_HEADER_TOKEN` 是否与网关一致。

- **现象：网关返回 `iam authorize unavailable`（503）**
  - 检查 IAM 是否启动、`IAM_AUTHORIZE_CHECK_URL` 是否可达。
  - 检查网关与 IAM 的 `IAM_INTERNAL_ACCESS_TOKEN` 是否一致。

- **现象：授权总是落到 default 租户**
  - 检查 IAM 发出的 JWT 是否包含 `tenant_code` claim。
  - 检查网关是否已部署包含 `tenant_code -> X-Bids-Tenant` 映射的版本。

- **现象：前端跳转到 5173 的 `/iam/login` 404**
  - 检查 `vite.config.js` 是否启用了 `/iam` 到 `8080` 的代理。
  - 或显式配置 `VITE_YEDS_LOGIN_URL=http://127.0.0.1:8080/iam/login`。
