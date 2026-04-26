# Docker Compose 部署文档（MVP）

## 1. 部署目标

使用 `docker-compose` 在本地快速拉起 MVP 所需基础组件与核心微服务，用于联调与演示。

## 2. 服务清单

- 基础组件：`mysql`、`redis`、`nacos`、`rocketmq-namesrv`、`rocketmq-broker`、`prometheus`、`grafana`  
- 业务服务：`gateway-dataplane`、`gateway-controlplane`、`api-management`、`auth-service`、`app-portal-service`、`observability-service`  

## 3. 目录建议

```text
deploy/
  docker-compose.yml
  prometheus/
    prometheus.yml
  mysql/
    init.sql
```

## 4. docker-compose.yml 示例

```yaml
version: "3.9"

services:
  mysql:
    image: mysql:8.0
    container_name: apig-mysql
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: apig
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./mysql/init.sql:/docker-entrypoint-initdb.d/init.sql
    networks:
      - apig-net

  redis:
    image: redis:7
    container_name: apig-redis
    ports:
      - "6379:6379"
    networks:
      - apig-net

  nacos:
    image: nacos/nacos-server:v2.3.2
    container_name: apig-nacos
    environment:
      MODE: standalone
      PREFER_HOST_MODE: hostname
    ports:
      - "8848:8848"
    networks:
      - apig-net

  rocketmq-namesrv:
    image: apache/rocketmq:5.3.1
    container_name: apig-rmq-namesrv
    command: sh mqnamesrv
    ports:
      - "9876:9876"
    networks:
      - apig-net

  rocketmq-broker:
    image: apache/rocketmq:5.3.1
    container_name: apig-rmq-broker
    depends_on:
      - rocketmq-namesrv
    command: sh mqbroker -n rocketmq-namesrv:9876 --enable-proxy
    ports:
      - "10911:10911"
      - "10909:10909"
    networks:
      - apig-net

  prometheus:
    image: prom/prometheus:v2.53.0
    container_name: apig-prometheus
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
    ports:
      - "9090:9090"
    networks:
      - apig-net

  grafana:
    image: grafana/grafana:11.1.0
    container_name: apig-grafana
    ports:
      - "3000:3000"
    depends_on:
      - prometheus
    networks:
      - apig-net

  gateway-dataplane:
    image: apig/gateway-dataplane:latest
    container_name: gateway-dataplane
    depends_on:
      - nacos
      - redis
      - auth-service
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      NACOS_ADDR: nacos:8848
      REDIS_ADDR: redis:6379
      AUTH_SERVICE_URL: http://auth-service:8083
    networks:
      - apig-net

  gateway-controlplane:
    image: apig/gateway-controlplane:latest
    container_name: gateway-controlplane
    depends_on:
      - mysql
      - nacos
      - rocketmq-broker
    ports:
      - "8081:8081"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      MYSQL_URL: jdbc:mysql://mysql:3306/apig
      MYSQL_USER: root
      MYSQL_PASSWORD: root
      NACOS_ADDR: nacos:8848
      ROCKETMQ_NAMESRV: rocketmq-namesrv:9876
    networks:
      - apig-net

  api-management:
    image: apig/api-management:latest
    container_name: api-management
    depends_on:
      - mysql
      - nacos
    ports:
      - "8082:8082"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      MYSQL_URL: jdbc:mysql://mysql:3306/apig
      MYSQL_USER: root
      MYSQL_PASSWORD: root
      NACOS_ADDR: nacos:8848
    networks:
      - apig-net

  auth-service:
    image: apig/auth-service:latest
    container_name: auth-service
    depends_on:
      - mysql
      - redis
      - nacos
    ports:
      - "8083:8083"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      MYSQL_URL: jdbc:mysql://mysql:3306/apig
      MYSQL_USER: root
      MYSQL_PASSWORD: root
      REDIS_ADDR: redis:6379
      NACOS_ADDR: nacos:8848
    networks:
      - apig-net

  app-portal-service:
    image: apig/app-portal-service:latest
    container_name: app-portal-service
    depends_on:
      - mysql
      - nacos
      - auth-service
    ports:
      - "8084:8084"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      MYSQL_URL: jdbc:mysql://mysql:3306/apig
      MYSQL_USER: root
      MYSQL_PASSWORD: root
      NACOS_ADDR: nacos:8848
      AUTH_SERVICE_URL: http://auth-service:8083
    networks:
      - apig-net

  observability-service:
    image: apig/observability-service:latest
    container_name: observability-service
    depends_on:
      - nacos
      - rocketmq-broker
    ports:
      - "8085:8085"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      NACOS_ADDR: nacos:8848
      ROCKETMQ_NAMESRV: rocketmq-namesrv:9876
      PROMETHEUS_ADDR: http://prometheus:9090
    networks:
      - apig-net

networks:
  apig-net:
    driver: bridge

volumes:
  mysql_data:
```

## 5. 启停命令

```bash
docker compose -f deploy/docker-compose.yml up -d
docker compose -f deploy/docker-compose.yml ps
docker compose -f deploy/docker-compose.yml logs -f gateway-dataplane
docker compose -f deploy/docker-compose.yml down
```

## 6. 访问入口

- 网关入口：`http://localhost:8080`
- Nacos：`http://localhost:8848`
- Prometheus：`http://localhost:9090`
- Grafana：`http://localhost:3000`
