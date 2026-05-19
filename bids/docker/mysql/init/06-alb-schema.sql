SET NAMES utf8mb4;
USE alb;

CREATE TABLE IF NOT EXISTS alb_upstream (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name              VARCHAR(64)  NOT NULL COMMENT '上游名称',
    target_url        VARCHAR(512) NOT NULL COMMENT '目标URL',
    websocket_enabled TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '是否启用WebSocket',
    remark            VARCHAR(256) COMMENT '备注',
    gmt_create        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    CONSTRAINT uk_alb_upstream_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ALB上游服务';

CREATE TABLE IF NOT EXISTS alb_route (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    env             VARCHAR(16)  NOT NULL DEFAULT 'dev' COMMENT '环境标识',
    host            VARCHAR(128) NOT NULL COMMENT '匹配主机名',
    path_pattern    VARCHAR(256) NOT NULL DEFAULT '/**' COMMENT '路径匹配模式',
    priority        INT NOT NULL DEFAULT 0 COMMENT '优先级，数值越大越优先',
    upstream_id     BIGINT NOT NULL COMMENT '上游ID',
    enabled         TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '是否启用',
    strip_prefix    TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否剥离路径前缀',
    redirect_url    VARCHAR(512) COMMENT '重定向URL',
    system_locked   TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '系统锁定不可删',
    remark          VARCHAR(256) COMMENT '备注',
    gmt_create      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    CONSTRAINT fk_alb_route_upstream FOREIGN KEY (upstream_id) REFERENCES alb_upstream(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ALB路由规则';

CREATE INDEX idx_alb_route_host ON alb_route (host, enabled, priority);

CREATE TABLE IF NOT EXISTS alb_header_policy (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    route_id     BIGINT NOT NULL COMMENT '路由ID',
    direction    VARCHAR(16) NOT NULL COMMENT '方向：REQUEST,RESPONSE',
    op           VARCHAR(16) NOT NULL COMMENT '操作：SET,ADD,REMOVE',
    header_key   VARCHAR(128) NOT NULL COMMENT 'Header键',
    header_value VARCHAR(512) COMMENT 'Header值',
    sort_order   INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    enabled      TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '是否启用',
    gmt_create   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    CONSTRAINT fk_alb_header_route FOREIGN KEY (route_id) REFERENCES alb_route(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ALB Header策略';

CREATE TABLE IF NOT EXISTS alb_route_release (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    version       INT NOT NULL COMMENT '发布版本号',
    snapshot_json LONGTEXT NOT NULL COMMENT '路由快照JSON',
    caddy_config  LONGTEXT NOT NULL COMMENT 'Caddy配置内容',
    published_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ALB路由发布记录';
