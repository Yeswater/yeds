# bids

## Docker

编排已迁移至 **yeds 仓库根**（项目名 `yeds`），见 [env/构建环境-全栈Docker模式.md](../env/构建环境-全栈Docker模式.md)。

```bash
cd <yeds 根>
docker compose -f docker-compose.yml -f docker-compose.macos.yml up -d
# up 会自动构建 jar 与前端 dist；亦可先 ./deploy/scripts/build-yeds-artifacts.sh
```

本地 `mvn spring-boot:run` 前：`cd ../foundation && mvn -DskipTests install`。
