# 数据库 DDL 注释规范

## MySQL

- 每个字段使用行尾 `COMMENT '中文说明'`。
- 每张表在 `CREATE TABLE` 闭合处使用 `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表说明'`。
- 状态、枚举类字段在 COMMENT 中列出常见取值，例如：`COMMENT 'PENDING,RUNNING,SUCCESS,FAILED,CANCELLED'`。
- 种子数据脚本（`INSERT`）与运维补丁脚本不写 COMMENT。

## PostgreSQL

- 建表后使用 `COMMENT ON TABLE ... IS '...'` 与 `COMMENT ON COLUMN ... IS '...'`。
- 演示库脚本见 `deploy/docker/pgsql/init/`。

## 单一真相源

| 环境 | 权威路径 |
|------|----------|
| Docker MySQL（bids / iam / alb） | `deploy/docker/mysql/init/` |
| Docker PostgreSQL 演示 | `deploy/docker/pgsql/init/` |
| 各后端 classpath `schema.sql` | 与上述 init 保持内容一致，供查阅与 diff；运行时默认 `spring.sql.init.mode=never` |

修改表结构时：先改 Docker init，再同步对应模块的 `schema.sql`。
