SET NAMES utf8mb4;
USE alb;

INSERT INTO alb_upstream (id, name, target_url, websocket_enabled, remark) VALUES
(1, 'apig', 'http://127.0.0.1:8080', 1, 'API 网关（剥 /apig 前缀）'),
(2, 'bids-fe', 'http://127.0.0.1:5173/bids', 1, 'BIDS 前端 Vite'),
(3, 'iam-fe', 'http://127.0.0.1:5181/iam', 1, 'IAM 管理台 Vite'),
(4, 'iam-login', 'http://127.0.0.1:8091/iam/login', 0, '统一登录静态页'),
(5, 'alb-console', 'http://127.0.0.1:5185/alb', 1, 'ALB 控制台'),
(6, 'alb-cp-api', 'http://127.0.0.1:8095/api/alb', 0, 'ALB 控制面 API')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    target_url = VALUES(target_url),
    websocket_enabled = VALUES(websocket_enabled),
    remark = VALUES(remark);

INSERT INTO alb_route (id, env, host, path_pattern, priority, upstream_id, enabled, redirect_url, system_locked, remark) VALUES
(1, 'dev', 'yeds.com', '/apig/iam/login/**', 200, 4, 1, NULL, 1, '统一登录'),
(2, 'dev', 'yeds.com', '/apig/**', 100, 1, 1, NULL, 1, '北向 APIG'),
(3, 'dev', 'yeds.com', '/bids/**', 100, 2, 1, NULL, 1, 'BIDS 控制台'),
(4, 'dev', 'yeds.com', '/iam/**', 100, 3, 1, NULL, 1, 'IAM 管理台'),
(5, 'dev', 'yeds.com', '/alb/**', 100, 5, 1, NULL, 1, 'ALB 控制台'),
(6, 'dev', 'yeds.com', '/alb-api/**', 100, 6, 1, NULL, 1, 'ALB 控制面 API'),
(7, 'dev', 'localhost', '/apig/iam/login/**', 200, 4, 1, NULL, 1, 'localhost 登录'),
(8, 'dev', 'localhost', '/apig/**', 100, 1, 1, NULL, 1, 'localhost APIG'),
(9, 'dev', 'localhost', '/bids/**', 100, 2, 1, NULL, 1, 'localhost BIDS'),
(10, 'dev', 'localhost', '/iam/**', 100, 3, 1, NULL, 1, 'localhost IAM'),
(11, 'dev', 'localhost', '/alb/**', 100, 5, 1, NULL, 1, 'localhost ALB 控制台'),
(12, 'dev', 'localhost', '/alb-api/**', 100, 6, 1, NULL, 1, 'localhost ALB API')
ON DUPLICATE KEY UPDATE
    env = VALUES(env),
    host = VALUES(host),
    path_pattern = VALUES(path_pattern),
    priority = VALUES(priority),
    upstream_id = VALUES(upstream_id),
    enabled = VALUES(enabled),
    redirect_url = VALUES(redirect_url),
    system_locked = VALUES(system_locked),
    remark = VALUES(remark);
