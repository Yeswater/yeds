# 后端异常日志 SDK 接入说明

## 1. 目标

- 统一 `bids-config`、`bids-exec`、`iam-backend` 的异常返回格式。
- 确保前端收到异常时，后端日志有可检索记录。
- 统一透传 `traceId`，支持前后端串联排障。

## 2. SDK 模块位置

- 模块：`foundation/common-web-starter`（Maven 坐标：`com.yeswater.foundation:common-web-starter`）
- 核心能力：
  - `ApiException`：统一业务异常类型
  - `DefaultGlobalExceptionHandler`：统一异常处理与日志策略
  - `TraceIdFilter`：生成/透传 `traceId`，写入 MDC 并回写响应头
  - `ExceptionMapper`：模块化异常扩展点

## 3. 默认行为

- 业务异常（如 `ApiException`、扩展映射为 `business=true`）：
  - 默认 `WARN` 日志。
  - 默认不打印 stacktrace（可配置打开）。
- 系统异常（未映射异常）：
  - `ERROR` 日志，包含 stacktrace。
  - 返回统一文案 `服务内部异常`（可配置）。
- 响应体字段：
  - `code`、`message`、`traceId`、`timestamp`

## 4. 配置项

前缀：`yeds.web.error`

- `internal-message`：系统异常对外文案，默认 `服务内部异常`
- `trace-header-name`：trace 头名称，默认 `X-Trace-Id`
- `business-stack-trace-enabled`：业务异常是否打印堆栈，默认 `false`

示例：

```yaml
yeds:
  web:
    error:
      internal-message: 服务内部异常
      trace-header-name: X-Trace-Id
      business-stack-trace-enabled: false
```

## 5. 各服务接入方式

### 5.1 bids-config

- 依赖 `com.yeswater.foundation:common-web-starter`
- 删除本地 `GlobalExceptionHandler` 与 `ApiException`
- 业务代码统一改为 `com.yeswater.foundation.common.web.ApiException`

### 5.2 bids-exec

- 依赖 `com.yeswater.foundation:common-web-starter`
- 删除本地 `GlobalExceptionHandler` 与 `ApiException`
- 增加 `ExportExceptionMapper`，将 `ExportException` 映射到统一响应

### 5.3 iam-backend

- 依赖 `com.yeswater.foundation:common-web-starter`
- 删除本地 `GlobalExceptionHandler`
- 增加 `BusinessExceptionMapper`，将 `BusinessException` 映射为 400
- `InternalAccessFilter` 拒绝分支新增 `WARN` 日志（含 `traceId`）

## 6. 验证清单

编译验证：

```bash
cd foundation && mvn -q -DskipTests install
cd bids/backend && mvn -q -DskipTests install
cd iam/backend && mvn -q -DskipTests compile
```

手工验证建议：

1. 触发一个参数校验失败（4xx）请求，确认：
   - 前端收到统一错误体；
   - 后端出现 `WARN` 日志，包含 `traceId`。
2. 触发一个未捕获异常（5xx）请求，确认：
   - 前端收到 `服务内部异常`；
   - 后端出现 `ERROR` 日志，含 stacktrace 与 `traceId`。
3. 触发 IAM 内部访问拒绝，确认：
   - 返回 403；
   - `InternalAccessFilter` 记录拒绝日志与 `traceId`。
