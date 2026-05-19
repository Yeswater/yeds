SET NAMES utf8mb4;
USE iam;

create table iam_user
(
    id            bigint primary key auto_increment comment '主键ID',
    username      varchar(64)  not null comment '登录用户名',
    password      varchar(128) not null comment '登录密码',
    display_name  varchar(128) not null comment '显示名称',
    status        tinyint      not null default 1 comment '状态：1启用0禁用',
    mfa_enabled   tinyint      not null default 0 comment '是否启用MFA',
    risk_level    varchar(16)  not null default 'LOW' comment '风险等级：LOW,MEDIUM,HIGH',
    tenant_code   varchar(64)  not null default 'default' comment '租户编码',
    department    varchar(64)  not null default 'general' comment '部门',
    last_login_ip varchar(64) comment '最近登录IP',
    last_login_at datetime comment '最近登录时间',
    gmt_create    datetime     not null comment '创建时间',
    gmt_modified  datetime     not null comment '修改时间'
) engine=InnoDB default charset=utf8mb4 comment='IAM用户';

create unique index uk_iam_user_username on iam_user (username);

create table iam_role
(
    id           bigint primary key auto_increment comment '主键ID',
    role_code    varchar(64)  not null comment '角色编码',
    role_name    varchar(128) not null comment '角色名称',
    gmt_create   datetime     not null comment '创建时间',
    gmt_modified datetime     not null comment '修改时间'
) engine=InnoDB default charset=utf8mb4 comment='IAM角色';

create unique index uk_iam_role_code on iam_role (role_code);

create table iam_permission
(
    id              bigint primary key auto_increment comment '主键ID',
    permission_code varchar(128) not null comment '权限编码',
    resource_code   varchar(128) not null comment '资源编码',
    action_code     varchar(64)  not null comment '动作编码',
    gmt_create      datetime     not null comment '创建时间',
    gmt_modified    datetime     not null comment '修改时间'
) engine=InnoDB default charset=utf8mb4 comment='IAM权限点';

create unique index uk_iam_permission_code on iam_permission (permission_code);
create index idx_iam_permission_resource_action on iam_permission (resource_code, action_code);

create table iam_user_role
(
    id           bigint primary key auto_increment comment '主键ID',
    user_id      bigint   not null comment '用户ID',
    role_id      bigint   not null comment '角色ID',
    gmt_create   datetime not null comment '创建时间',
    gmt_modified datetime not null comment '修改时间'
) engine=InnoDB default charset=utf8mb4 comment='用户角色关联';

create unique index uk_iam_user_role on iam_user_role (user_id, role_id);
create index idx_iam_user_role_user on iam_user_role (user_id);

create table iam_role_permission
(
    id            bigint primary key auto_increment comment '主键ID',
    role_id       bigint   not null comment '角色ID',
    permission_id bigint   not null comment '权限ID',
    gmt_create    datetime not null comment '创建时间',
    gmt_modified  datetime not null comment '修改时间'
) engine=InnoDB default charset=utf8mb4 comment='角色权限关联';

create unique index uk_iam_role_permission on iam_role_permission (role_id, permission_id);
create index idx_iam_role_permission_role on iam_role_permission (role_id);

create table iam_role_policy
(
    id           bigint primary key auto_increment comment '主键ID',
    role_id      bigint   not null comment '角色ID',
    policy_id    bigint   not null comment '策略ID',
    gmt_create   datetime not null comment '创建时间',
    gmt_modified datetime not null comment '修改时间'
) engine=InnoDB default charset=utf8mb4 comment='角色策略关联';

create unique index uk_iam_role_policy on iam_role_policy (role_id, policy_id);
create index idx_iam_role_policy_role on iam_role_policy (role_id);

create table iam_token_session
(
    id            bigint primary key auto_increment comment '主键ID',
    user_id       bigint       not null comment '用户ID',
    refresh_token varchar(128) not null comment '刷新令牌',
    expired_at    datetime     not null comment '过期时间',
    is_revoked    tinyint      not null default 0 comment '是否已吊销',
    gmt_create    datetime     not null comment '创建时间',
    gmt_modified  datetime     not null comment '修改时间'
) engine=InnoDB default charset=utf8mb4 comment='令牌会话';

create unique index uk_iam_token_session_refresh_token on iam_token_session (refresh_token);
create index idx_iam_token_session_user_id on iam_token_session (user_id);

create table iam_policy
(
    id           bigint primary key auto_increment comment '主键ID',
    policy_name  varchar(128) not null comment '策略名称',
    policy_type  varchar(64)  not null comment '策略类型：IP,TIME_WINDOW,ENV_TAG等',
    expression   varchar(512) not null comment '策略表达式',
    status       tinyint      not null default 1 comment '状态：1启用0禁用',
    gmt_create   datetime     not null comment '创建时间',
    gmt_modified datetime     not null comment '修改时间'
) engine=InnoDB default charset=utf8mb4 comment='访问策略';

create index idx_iam_policy_type on iam_policy (policy_type);

create table iam_mfa_otp
(
    id           bigint primary key auto_increment comment '主键ID',
    user_id      bigint      not null comment '用户ID',
    otp_code     varchar(12) not null comment 'OTP验证码',
    expired_at   datetime    not null comment '过期时间',
    used         tinyint     not null default 0 comment '是否已使用',
    gmt_create   datetime    not null comment '创建时间',
    gmt_modified datetime    not null comment '修改时间'
) engine=InnoDB default charset=utf8mb4 comment='MFA一次性密码';

create index idx_iam_mfa_otp_user_time on iam_mfa_otp (user_id, expired_at);

create table iam_client
(
    id               bigint primary key auto_increment comment '主键ID',
    client_id        varchar(64)  not null comment '客户端ID',
    client_name      varchar(128) not null comment '客户端名称',
    client_secret    varchar(128) not null comment '客户端密钥',
    prev_secret      varchar(128) comment '上一版密钥',
    secret_rotate_at datetime comment '密钥轮换时间',
    last_used_at     datetime comment '最近使用时间',
    status           tinyint      not null default 1 comment '状态：1启用0禁用',
    gmt_create       datetime     not null comment '创建时间',
    gmt_modified     datetime     not null comment '修改时间'
) engine=InnoDB default charset=utf8mb4 comment='OAuth客户端';

create unique index uk_iam_client_client_id on iam_client (client_id);

create table iam_oidc_identity
(
    id               bigint primary key auto_increment comment '主键ID',
    issuer           varchar(128) not null comment 'OIDC发行方',
    external_subject varchar(128) not null comment '外部主体标识',
    user_id          bigint       not null comment '本地用户ID',
    gmt_create       datetime     not null comment '创建时间',
    gmt_modified     datetime     not null comment '修改时间'
) engine=InnoDB default charset=utf8mb4 comment='OIDC身份映射';

create unique index uk_iam_oidc_identity on iam_oidc_identity (issuer, external_subject);

create table iam_tenant_federation
(
    id              bigint primary key auto_increment comment '主键ID',
    tenant_code     varchar(64)  not null comment '租户编码',
    issuer          varchar(128) not null comment 'OIDC发行方',
    external_tenant varchar(128) not null comment '外部租户标识',
    app_code        varchar(32)  not null default 'IAM' comment '应用编码',
    status          tinyint      not null default 1 comment '状态：1启用0禁用',
    modified_by     varchar(64)  not null default 'system' comment '最后修改人',
    gmt_create      datetime     not null comment '创建时间',
    gmt_modified    datetime     not null comment '修改时间'
) engine=InnoDB default charset=utf8mb4 comment='租户联邦映射';

create unique index uk_iam_tenant_federation on iam_tenant_federation (tenant_code, issuer, external_tenant);

create table iam_tenant
(
    id           bigint primary key auto_increment comment '主键ID',
    tenant_code  varchar(64)  not null comment '租户编码',
    tenant_name  varchar(128) not null comment '租户名称',
    status       tinyint      not null default 1 comment '状态：1启用0禁用',
    gmt_create   datetime     not null comment '创建时间',
    gmt_modified datetime     not null comment '修改时间'
) engine=InnoDB default charset=utf8mb4 comment='租户主数据';

create unique index uk_iam_tenant_code on iam_tenant (tenant_code);

create table iam_oidc_auth_code
(
    id               bigint primary key auto_increment comment '主键ID',
    auth_code        varchar(64)  not null comment '授权码',
    issuer           varchar(128) not null comment 'OIDC发行方',
    external_subject varchar(128) not null comment '外部主体标识',
    user_id          bigint       not null comment '本地用户ID',
    expired_at       datetime     not null comment '过期时间',
    used             tinyint      not null default 0 comment '是否已使用',
    gmt_create       datetime     not null comment '创建时间',
    gmt_modified     datetime     not null comment '修改时间'
) engine=InnoDB default charset=utf8mb4 comment='OIDC授权码';

create unique index uk_iam_oidc_auth_code on iam_oidc_auth_code (auth_code);
create index idx_iam_oidc_auth_code_user on iam_oidc_auth_code (user_id, expired_at);

create table iam_permission_change_request
(
    id               bigint primary key auto_increment comment '主键ID',
    role_id          bigint        not null comment '角色ID',
    permission_codes varchar(1024) not null comment '申请权限编码列表',
    status           varchar(16)   not null comment '状态：PENDING,APPROVED,REJECTED',
    requested_by     varchar(64)   not null comment '申请人',
    approved_by      varchar(64) comment '审批人',
    approved_at      datetime comment '审批时间',
    approval_comment varchar(512) comment '审批意见',
    gmt_create       datetime      not null comment '创建时间',
    gmt_modified     datetime      not null comment '修改时间'
) engine=InnoDB default charset=utf8mb4 comment='权限变更申请';

create index idx_iam_permission_change_request_role on iam_permission_change_request (role_id, status);

create table iam_abac_policy
(
    id            bigint primary key auto_increment comment '主键ID',
    policy_name   varchar(128) not null comment '策略名称',
    resource_code varchar(128) not null comment '资源编码',
    action_code   varchar(64)  not null comment '动作编码',
    expression    varchar(512) not null comment 'ABAC表达式',
    app_code      varchar(32)  not null default 'IAM' comment '所属应用',
    status        tinyint      not null default 1 comment '状态：1启用0禁用',
    created_by    varchar(64)  not null comment '创建人',
    owner         varchar(64)  not null comment '责任人',
    modified_by   varchar(64)  not null comment '最后修改人',
    gmt_create    datetime     not null comment '创建时间',
    gmt_modified  datetime     not null comment '修改时间'
) engine=InnoDB default charset=utf8mb4 comment='ABAC策略';

create index idx_iam_abac_policy_resource_action on iam_abac_policy (resource_code, action_code, status);

create table iam_risk_event
(
    id          bigint primary key auto_increment comment '主键ID',
    event_type  varchar(64)  not null comment '事件类型',
    user_id     bigint comment '用户ID',
    tenant_code varchar(64) comment '租户编码',
    severity    varchar(16)  not null comment '严重级别',
    detail      varchar(512) not null comment '事件详情',
    status      varchar(16)  not null default 'OPEN' comment '状态：OPEN,CLOSED',
    gmt_create  datetime     not null comment '创建时间',
    gmt_modified datetime    not null comment '修改时间'
) engine=InnoDB default charset=utf8mb4 comment='风险事件';

create index idx_iam_risk_event_time on iam_risk_event (gmt_create, severity);
create index idx_iam_risk_event_user on iam_risk_event (user_id, event_type);

create table iam_audit_log
(
    id            bigint primary key auto_increment comment '主键ID',
    event_type    varchar(64)  not null comment '事件类型',
    username      varchar(64) comment '用户名',
    user_id       bigint comment '用户ID',
    resource_code varchar(128) comment '资源编码',
    action_code   varchar(64) comment '动作编码',
    decision      varchar(32)  not null comment '决策结果',
    detail        varchar(512) not null comment '详情',
    client_ip     varchar(64) comment '客户端IP',
    gmt_create    datetime     not null comment '创建时间',
    gmt_modified  datetime     not null comment '修改时间'
) engine=InnoDB default charset=utf8mb4 comment='审计日志';

create index idx_iam_audit_log_user_time on iam_audit_log (username, gmt_create);
create index idx_iam_audit_log_type_time on iam_audit_log (event_type, gmt_create);
