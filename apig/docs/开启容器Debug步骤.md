# 开启容器 Debug 步骤

## 1. 服务镜像增加调试端口

### 1.1 `auth-service/Dockerfile`
- 暴露业务端口：`8083`
- 暴露调试端口：`5005`

### 1.2 `gateway-dataplane/Dockerfile`
- 暴露业务端口：`8080`
- 暴露调试端口：`5006`

## 2. Docker Compose 开启 JDWP

修改 `deploy/docker-compose.yml`：

### 2.1 `auth-service`
- 端口映射增加：`5005:5005`
- 环境变量增加：
  - `JAVA_TOOL_OPTIONS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005`

### 2.2 `gateway-dataplane`
- 端口映射增加：`5006:5006`
- 环境变量增加：
  - `JAVA_TOOL_OPTIONS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5006`

## 3. 修复容器 Jar 启动问题

问题现象：`no main manifest attribute, in /app/app.jar`

修复方式：
- 在 `auth-service/pom.xml` 的 `spring-boot-maven-plugin` 增加 `repackage`
- 在 `gateway-dataplane/pom.xml` 的 `spring-boot-maven-plugin` 增加 `repackage`

## 4. 编译调试信息配置

修改根 `pom.xml`：
- `java.version` 调整为 `21`
- `maven-compiler-plugin` 增加：
  - `<debug>true</debug>`
  - `<parameters>true</parameters>`

## 5. Cursor 调试配置

### 5.1 `/.vscode/launch.json`
新增/修改 attach 配置（auth-service）：
- `type: java`
- `request: attach`
- `hostName: localhost`
- `port: 5005`
- `projectName: auth-service`
- `sourcePaths: ["${workspaceFolder}/auth-service/src/main/java"]`

### 5.2 `/.vscode/settings.json`
- `java.configuration.maven.downloadSources: true`
- `java.debug.settings.onBuildFailureProceed: true`

## 6. 下载依赖源码

执行：

```bash
mvn -DskipTests dependency:sources
```

## 7. 构建并启动调试容器

执行：

```bash
docker compose -f deploy/docker-compose.yml up -d --build auth-service gateway-dataplane
```

## 8. 启动成功判定

查看日志，出现以下内容表示调试端口生效：

- auth-service：`Listening for transport dt_socket at address: 5005`
- gateway-dataplane：`Listening for transport dt_socket at address: 5006`

## 9. 在 Cursor 中 Attach

- 连接 `auth-service`：`localhost:5005`
- 连接 `gateway-dataplane`：`localhost:5006`

断点命中后可查看变量、调用栈并单击进入源码。  

## 10. 添加 `.vscode\settings.json`
```json
{
    "java.compile.nullAnalysis.mode": "automatic",
    "java.jdt.ls.java.home": "/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home",
    "java.configuration.runtimes": [
        {
            "name": "JavaSE-21",
            "path": "/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home",
            "default": true
        }
    ],
    "java.configuration.updateBuildConfiguration": "interactive",
    "java.import.maven.enabled": true,
    "java.maven.downloadSources": true
}
```
