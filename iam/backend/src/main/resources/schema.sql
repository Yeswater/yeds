create table iam_user
(
    id           bigint primary key auto_increment,
    username     varchar(64)  not null,
    password     varchar(128) not null,
    display_name varchar(128) not null,
    status       tinyint      not null default 1,
    mfa_enabled  tinyint      not null default 0,
    risk_level   varchar(16)  not null default 'LOW',
    tenant_code  varchar(64)  not null default 'default',
    department   varchar(64)  not null default 'general',
    last_login_ip varchar(64),
    last_login_at datetime,
    gmt_create   datetime     not null,
    gmt_modified datetime     not null
);

create unique index uk_iam_user_username on iam_user (username);

create table iam_role
(
    id           bigint primary key auto_increment,
    role_code    varchar(64)  not null,
    role_name    varchar(128) not null,
    gmt_create   datetime     not null,
    gmt_modified datetime     not null
);

create unique index uk_iam_role_code on iam_role (role_code);

create table iam_permission
(
    id              bigint primary key auto_increment,
    permission_code varchar(128) not null,
    resource_code   varchar(128) not null,
    action_code     varchar(64)  not null,
    gmt_create      datetime     not null,
    gmt_modified    datetime     not null
);

create unique index uk_iam_permission_code on iam_permission (permission_code);
create index idx_iam_permission_resource_action on iam_permission (resource_code, action_code);

create table iam_user_role
(
    id           bigint primary key auto_increment,
    user_id      bigint   not null,
    role_id      bigint   not null,
    gmt_create   datetime not null,
    gmt_modified datetime not null
);

create unique index uk_iam_user_role on iam_user_role (user_id, role_id);
create index idx_iam_user_role_user on iam_user_role (user_id);

create table iam_role_permission
(
    id            bigint primary key auto_increment,
    role_id        bigint   not null,
    permission_id  bigint   not null,
    gmt_create     datetime not null,
    gmt_modified   datetime not null
);

create unique index uk_iam_role_permission on iam_role_permission (role_id, permission_id);
create index idx_iam_role_permission_role on iam_role_permission (role_id);

create table iam_role_policy
(
    id           bigint primary key auto_increment,
    role_id       bigint   not null,
    policy_id     bigint   not null,
    gmt_create    datetime not null,
    gmt_modified  datetime not null
);

create unique index uk_iam_role_policy on iam_role_policy (role_id, policy_id);
create index idx_iam_role_policy_role on iam_role_policy (role_id);

create table iam_token_session
(
    id            bigint primary key auto_increment,
    user_id        bigint       not null,
    refresh_token  varchar(128) not null,
    expired_at     datetime     not null,
    is_revoked     tinyint      not null default 0,
    gmt_create     datetime     not null,
    gmt_modified   datetime     not null
);

create unique index uk_iam_token_session_refresh_token on iam_token_session (refresh_token);
create index idx_iam_token_session_user_id on iam_token_session (user_id);

create table iam_policy
(
    id            bigint primary key auto_increment,
    policy_name    varchar(128) not null,
    policy_type    varchar(64)  not null,
    expression     varchar(512) not null,
    status         tinyint      not null default 1,
    gmt_create     datetime     not null,
    gmt_modified   datetime     not null
);

create index idx_iam_policy_type on iam_policy (policy_type);

create table iam_mfa_otp
(
    id            bigint primary key auto_increment,
    user_id        bigint      not null,
    otp_code       varchar(12) not null,
    expired_at     datetime    not null,
    used           tinyint     not null default 0,
    gmt_create     datetime    not null,
    gmt_modified   datetime    not null
);

create index idx_iam_mfa_otp_user_time on iam_mfa_otp (user_id, expired_at);

create table iam_client
(
    id             bigint primary key auto_increment,
    client_id       varchar(64)  not null,
    client_name     varchar(128) not null,
    client_secret   varchar(128) not null,
    prev_secret     varchar(128),
    secret_rotate_at datetime,
    last_used_at    datetime,
    status          tinyint      not null default 1,
    gmt_create      datetime     not null,
    gmt_modified    datetime     not null
);

create unique index uk_iam_client_client_id on iam_client (client_id);

create table iam_oidc_identity
(
    id               bigint primary key auto_increment,
    issuer           varchar(128) not null,
    external_subject varchar(128) not null,
    user_id          bigint       not null,
    gmt_create       datetime     not null,
    gmt_modified     datetime     not null
);

create unique index uk_iam_oidc_identity on iam_oidc_identity (issuer, external_subject);

create table iam_tenant_federation
(
    id               bigint primary key auto_increment,
    tenant_code      varchar(64)  not null,
    issuer           varchar(128) not null,
    external_tenant  varchar(128) not null,
    status           tinyint      not null default 1,
    gmt_create       datetime     not null,
    gmt_modified     datetime     not null
);

create unique index uk_iam_tenant_federation on iam_tenant_federation (tenant_code, issuer, external_tenant);

create table iam_tenant
(
    id             bigint primary key auto_increment,
    tenant_code    varchar(64)  not null,
    tenant_name    varchar(128) not null,
    status         tinyint      not null default 1,
    gmt_create     datetime     not null,
    gmt_modified   datetime     not null
);

create unique index uk_iam_tenant_code on iam_tenant (tenant_code);

create table iam_oidc_auth_code
(
    id               bigint primary key auto_increment,
    auth_code        varchar(64)  not null,
    issuer           varchar(128) not null,
    external_subject varchar(128) not null,
    user_id          bigint       not null,
    expired_at       datetime     not null,
    used             tinyint      not null default 0,
    gmt_create       datetime     not null,
    gmt_modified     datetime     not null
);

create unique index uk_iam_oidc_auth_code on iam_oidc_auth_code (auth_code);
create index idx_iam_oidc_auth_code_user on iam_oidc_auth_code (user_id, expired_at);

create table iam_permission_change_request
(
    id                    bigint primary key auto_increment,
    role_id               bigint       not null,
    permission_codes      varchar(1024) not null,
    status                varchar(16)  not null,
    requested_by          varchar(64)  not null,
    approved_by           varchar(64),
    approved_at           datetime,
    approval_comment      varchar(512),
    gmt_create            datetime     not null,
    gmt_modified          datetime     not null
);

create index idx_iam_permission_change_request_role on iam_permission_change_request (role_id, status);

create table iam_abac_policy
(
    id              bigint primary key auto_increment,
    policy_name     varchar(128) not null,
    resource_code   varchar(128) not null,
    action_code     varchar(64)  not null,
    expression      varchar(512) not null,
    status          tinyint      not null default 1,
    created_by      varchar(64)  not null,
    owner           varchar(64)  not null,
    modified_by     varchar(64)  not null,
    gmt_create      datetime     not null,
    gmt_modified    datetime     not null
);

create index idx_iam_abac_policy_resource_action on iam_abac_policy (resource_code, action_code, status);

create table iam_risk_event
(
    id             bigint primary key auto_increment,
    event_type     varchar(64)  not null,
    user_id        bigint,
    tenant_code    varchar(64),
    severity       varchar(16)  not null,
    detail         varchar(512) not null,
    status         varchar(16)  not null default 'OPEN',
    gmt_create     datetime     not null,
    gmt_modified   datetime     not null
);

create index idx_iam_risk_event_time on iam_risk_event (gmt_create, severity);
create index idx_iam_risk_event_user on iam_risk_event (user_id, event_type);

create table iam_audit_log
(
    id            bigint primary key auto_increment,
    event_type     varchar(64)  not null,
    username       varchar(64),
    user_id        bigint,
    resource_code  varchar(128),
    action_code    varchar(64),
    decision       varchar(32)  not null,
    detail         varchar(512) not null,
    client_ip      varchar(64),
    gmt_create     datetime     not null,
    gmt_modified   datetime     not null
);

create index idx_iam_audit_log_user_time on iam_audit_log (username, gmt_create);
create index idx_iam_audit_log_type_time on iam_audit_log (event_type, gmt_create);
