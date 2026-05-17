# auth-service + gateway-dataplane 主链路连通性测试

## 1. 启动（Docker）

```bash
docker compose -f deploy/docker-compose.yml up -d auth-service gateway-dataplane
```

## 2. 健康检查

```bash
curl -i http://localhost:8083/auth/health
curl -i http://localhost:8080/actuator/health
```

预期：两个接口都返回 `200`。

## 3. 认证签发 Token（auth-service）

```bash
curl -sS -X POST http://localhost:8083/auth/token \
  -H 'Content-Type: application/json' \
  -d '{"clientId":"portal-client","clientSecret":"portal-secret"}'
```

预期：返回 JSON，包含 `accessToken`、`tokenType`、`expiresIn`。

## 4. 网关公开路由（无需 Token）

```bash
curl -i http://localhost:8080/api/mock/public
```

预期：`200`，body 含 `gateway public api success`。

## 5. 网关受保护路由（带 Token）

```bash
TOKEN=$(curl -sS -X POST http://localhost:8083/auth/token \
  -H 'Content-Type: application/json' \
  -d '{"clientId":"portal-client","clientSecret":"portal-secret"}' | jq -r '.accessToken')

curl -i http://localhost:8080/api/mock/secure \
  -H "Authorization: Bearer $TOKEN"
```

预期：`200`，body 含：
- `message: gateway secure api success`
- `clientId: portal-client`
- `tenantId: tenant-a`

## 6. 反向用例（无 Token）

```bash
curl -i http://localhost:8080/api/mock/secure
```

预期：`401`（证明网关鉴权生效）。
