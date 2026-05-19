CREATE TABLE IF NOT EXISTS alb_upstream (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(64)  NOT NULL,
    target_url      VARCHAR(512) NOT NULL,
    websocket_enabled TINYINT UNSIGNED NOT NULL DEFAULT 1,
    remark          VARCHAR(256),
    gmt_create      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    gmt_modified    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_alb_upstream_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS alb_route (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    env             VARCHAR(16)  NOT NULL DEFAULT 'dev',
    host            VARCHAR(128) NOT NULL,
    path_pattern    VARCHAR(256) NOT NULL DEFAULT '/**',
    priority        INT NOT NULL DEFAULT 0,
    upstream_id     BIGINT NOT NULL,
    enabled         TINYINT UNSIGNED NOT NULL DEFAULT 1,
    strip_prefix    TINYINT UNSIGNED NOT NULL DEFAULT 0,
    redirect_url    VARCHAR(512),
    system_locked   TINYINT UNSIGNED NOT NULL DEFAULT 0,
    remark          VARCHAR(256),
    gmt_create      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    gmt_modified    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_alb_route_upstream FOREIGN KEY (upstream_id) REFERENCES alb_upstream(id)
);

CREATE INDEX IF NOT EXISTS idx_alb_route_host ON alb_route (host, enabled, priority);

CREATE TABLE IF NOT EXISTS alb_header_policy (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    route_id        BIGINT NOT NULL,
    direction       VARCHAR(16) NOT NULL,
    op              VARCHAR(16) NOT NULL,
    header_key      VARCHAR(128) NOT NULL,
    header_value    VARCHAR(512),
    sort_order      INT NOT NULL DEFAULT 0,
    enabled         TINYINT UNSIGNED NOT NULL DEFAULT 1,
    gmt_create      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    gmt_modified    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_alb_header_route FOREIGN KEY (route_id) REFERENCES alb_route(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS alb_route_release (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    version         INT NOT NULL,
    snapshot_json   CLOB NOT NULL,
    caddy_config    CLOB NOT NULL,
    published_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
