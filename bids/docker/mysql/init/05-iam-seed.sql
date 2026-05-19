SET NAMES utf8mb4;
USE iam;

insert into iam_user
(username, password, display_name, status, mfa_enabled, risk_level, tenant_code, department, last_login_ip, last_login_at, gmt_create, gmt_modified)
values ('admin', 'admin123', '系统管理员', 1, 0, 'HIGH', 'tenant-a', 'platform', '127.0.0.1', current_timestamp, current_timestamp, current_timestamp),
       ('auditor', 'auditor123', '审计用户', 1, 0, 'LOW', 'tenant-a', 'audit', null, null, current_timestamp, current_timestamp),
       ('analyst', 'analyst123', '分析用户', 1, 0, 'LOW', 'tenant-b', 'data', null, null, current_timestamp, current_timestamp);

insert into iam_role (role_code, role_name, gmt_create, gmt_modified)
values ('ADMIN', '管理员', current_timestamp, current_timestamp),
       ('VIEWER', '只读用户', current_timestamp, current_timestamp),
       ('AUDITOR', '审计员', current_timestamp, current_timestamp);

insert into iam_permission (permission_code, resource_code, action_code, gmt_create, gmt_modified)
values ('bids:model:read', 'bids:model', 'read', current_timestamp, current_timestamp),
       ('bids:model:execute', 'bids:model', 'execute', current_timestamp, current_timestamp),
       ('export:task:download', 'export:task', 'download', current_timestamp, current_timestamp),
       ('iam:policy:manage', 'iam:policy', 'manage', current_timestamp, current_timestamp);

insert into iam_user_role (user_id, role_id, gmt_create, gmt_modified)
select u.id, r.id, current_timestamp, current_timestamp
from iam_user u,
     iam_role r
where u.username = 'admin'
  and r.role_code = 'ADMIN';

insert into iam_user_role (user_id, role_id, gmt_create, gmt_modified)
select u.id, r.id, current_timestamp, current_timestamp
from iam_user u,
     iam_role r
where u.username = 'auditor'
  and r.role_code = 'AUDITOR';

insert into iam_role_permission (role_id, permission_id, gmt_create, gmt_modified)
select r.id, p.id, current_timestamp, current_timestamp
from iam_role r
         join iam_permission p on 1 = 1
where r.role_code = 'ADMIN';

insert into iam_role_permission (role_id, permission_id, gmt_create, gmt_modified)
select r.id, p.id, current_timestamp, current_timestamp
from iam_role r
         join iam_permission p on p.permission_code in ('bids:model:read', 'export:task:download')
where r.role_code = 'AUDITOR';

insert into iam_policy (policy_name, policy_type, expression, status, gmt_create, gmt_modified)
values ('办公网段白名单', 'IP', '127.0.0.1/32,10.10.0.0/16', 1, current_timestamp, current_timestamp),
       ('工作时段限制', 'TIME_WINDOW', '00:00-23:59', 1, current_timestamp, current_timestamp),
       ('生产环境标签限制', 'ENV_TAG', 'prod,staging', 1, current_timestamp, current_timestamp);

insert into iam_role_policy (role_id, policy_id, gmt_create, gmt_modified)
select r.id, p.id, current_timestamp, current_timestamp
from iam_role r
         join iam_policy p on p.policy_name in ('办公网段白名单', '工作时段限制')
where r.role_code = 'ADMIN';

insert into iam_oidc_identity (issuer, external_subject, user_id, gmt_create, gmt_modified)
select 'mock-oidc', 'external-admin', u.id, current_timestamp, current_timestamp
from iam_user u
where u.username = 'admin';

insert into iam_tenant_federation (tenant_code, issuer, external_tenant, app_code, status, modified_by, gmt_create, gmt_modified)
values ('tenant-a', 'mock-oidc', 'external-tenant-a', 'IAM', 1, 'admin', current_timestamp, current_timestamp),
       ('tenant-b', 'mock-oidc', 'external-tenant-b', 'IAM', 1, 'admin', current_timestamp, current_timestamp);

insert into iam_tenant (tenant_code, tenant_name, status, gmt_create, gmt_modified)
values ('tenant-a', 'A租户', 1, current_timestamp, current_timestamp),
       ('tenant-b', 'B租户', 1, current_timestamp, current_timestamp),
       ('tenant-c', 'C租户', 1, current_timestamp, current_timestamp);

insert into iam_abac_policy
(policy_name, resource_code, action_code, expression, app_code, status, created_by, owner, modified_by, gmt_create, gmt_modified)
values ('财务部门可执行模型', 'bids:model', 'execute', 'department=platform;envTag=prod', 'BIDS', 1, 'admin', 'admin', 'admin', current_timestamp, current_timestamp),
       ('租户隔离读取策略', 'bids:model', 'read', 'tenantCode=tenant-a', 'BIDS', 1, 'admin', 'admin', 'admin', current_timestamp, current_timestamp);
