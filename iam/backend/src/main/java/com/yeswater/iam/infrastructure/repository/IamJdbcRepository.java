package com.yeswater.iam.infrastructure.repository;

import com.yeswater.iam.domain.model.IamUserInfo;
import com.yeswater.iam.domain.model.ClientCredentialInfo;
import com.yeswater.iam.domain.model.AbacPolicyInfo;
import com.yeswater.iam.domain.model.OidcAuthCodeInfo;
import com.yeswater.iam.domain.model.PermissionChangeRequestInfo;
import com.yeswater.iam.domain.model.PolicyRuleInfo;
import com.yeswater.iam.domain.model.RiskEventInfo;
import com.yeswater.iam.domain.model.TenantFederationInfo;
import com.yeswater.iam.domain.model.TenantInfo;
import com.yeswater.iam.domain.model.TokenSessionInfo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class IamJdbcRepository {

    private static final RowMapper<IamUserInfo> USER_ROW_MAPPER = new RowMapper<>() {
        @Override
        public IamUserInfo mapRow(ResultSet resultSet, int rowNum) throws SQLException {
            return new IamUserInfo(
                    resultSet.getLong("id"),
                    resultSet.getString("username"),
                    resultSet.getString("password"),
                    resultSet.getString("display_name"),
                    resultSet.getInt("status"),
                    resultSet.getInt("mfa_enabled"),
                    resultSet.getString("risk_level"),
                    resultSet.getString("tenant_code"),
                    resultSet.getString("department"),
                    resultSet.getString("last_login_ip")
            );
        }
    };

    private static final RowMapper<AbacPolicyInfo> ABAC_POLICY_ROW_MAPPER = new RowMapper<>() {
        @Override
        public AbacPolicyInfo mapRow(ResultSet resultSet, int rowNum) throws SQLException {
            return new AbacPolicyInfo(
                    resultSet.getLong("id"),
                    resultSet.getString("policy_name"),
                    resultSet.getString("resource_code"),
                    resultSet.getString("action_code"),
                    resultSet.getString("expression"),
                    resultSet.getInt("status"),
                    resultSet.getString("created_by"),
                    resultSet.getString("owner"),
                    resultSet.getString("modified_by"),
                    resultSet.getTimestamp("gmt_create").toLocalDateTime(),
                    resultSet.getTimestamp("gmt_modified").toLocalDateTime()
            );
        }
    };

    private static final RowMapper<TenantInfo> TENANT_ROW_MAPPER = new RowMapper<>() {
        @Override
        public TenantInfo mapRow(ResultSet resultSet, int rowNum) throws SQLException {
            return new TenantInfo(
                    resultSet.getLong("id"),
                    resultSet.getString("tenant_code"),
                    resultSet.getString("tenant_name"),
                    resultSet.getInt("status")
            );
        }
    };

    private static final RowMapper<TokenSessionInfo> TOKEN_SESSION_ROW_MAPPER = new RowMapper<>() {
        @Override
        public TokenSessionInfo mapRow(ResultSet resultSet, int rowNum) throws SQLException {
            return new TokenSessionInfo(
                    resultSet.getLong("id"),
                    resultSet.getLong("user_id"),
                    resultSet.getString("refresh_token"),
                    resultSet.getTimestamp("expired_at").toLocalDateTime(),
                    resultSet.getInt("is_revoked")
            );
        }
    };

    private static final RowMapper<PolicyRuleInfo> POLICY_RULE_ROW_MAPPER = new RowMapper<>() {
        @Override
        public PolicyRuleInfo mapRow(ResultSet resultSet, int rowNum) throws SQLException {
            return new PolicyRuleInfo(
                    resultSet.getLong("id"),
                    resultSet.getString("policy_type"),
                    resultSet.getString("expression")
            );
        }
    };

    private static final RowMapper<ClientCredentialInfo> CLIENT_CREDENTIAL_ROW_MAPPER = new RowMapper<>() {
        @Override
        public ClientCredentialInfo mapRow(ResultSet resultSet, int rowNum) throws SQLException {
            Timestamp rotateAt = resultSet.getTimestamp("secret_rotate_at");
            return new ClientCredentialInfo(
                    resultSet.getLong("id"),
                    resultSet.getString("client_id"),
                    resultSet.getString("client_secret"),
                    resultSet.getString("prev_secret"),
                    rotateAt == null ? null : rotateAt.toLocalDateTime(),
                    resultSet.getInt("status")
            );
        }
    };

    private static final RowMapper<OidcAuthCodeInfo> OIDC_AUTH_CODE_ROW_MAPPER = new RowMapper<>() {
        @Override
        public OidcAuthCodeInfo mapRow(ResultSet resultSet, int rowNum) throws SQLException {
            return new OidcAuthCodeInfo(
                    resultSet.getLong("id"),
                    resultSet.getString("auth_code"),
                    resultSet.getString("issuer"),
                    resultSet.getString("external_subject"),
                    resultSet.getLong("user_id"),
                    resultSet.getTimestamp("expired_at").toLocalDateTime(),
                    resultSet.getInt("used")
            );
        }
    };

    private static final RowMapper<PermissionChangeRequestInfo> PERMISSION_CHANGE_REQUEST_ROW_MAPPER = new RowMapper<>() {
        @Override
        public PermissionChangeRequestInfo mapRow(ResultSet resultSet, int rowNum) throws SQLException {
            return new PermissionChangeRequestInfo(
                    resultSet.getLong("id"),
                    resultSet.getLong("role_id"),
                    resultSet.getString("permission_codes"),
                    resultSet.getString("status"),
                    resultSet.getString("requested_by")
            );
        }
    };

    private static final RowMapper<RiskEventInfo> RISK_EVENT_ROW_MAPPER = new RowMapper<>() {
        @Override
        public RiskEventInfo mapRow(ResultSet resultSet, int rowNum) throws SQLException {
            return new RiskEventInfo(
                    resultSet.getLong("id"),
                    resultSet.getString("event_type"),
                    resultSet.getObject("user_id") == null ? null : resultSet.getLong("user_id"),
                    resultSet.getString("tenant_code"),
                    resultSet.getString("severity"),
                    resultSet.getString("detail"),
                    resultSet.getString("status"),
                    resultSet.getTimestamp("gmt_create").toLocalDateTime()
            );
        }
    };

    private static final RowMapper<TenantFederationInfo> TENANT_FEDERATION_ROW_MAPPER = new RowMapper<>() {
        @Override
        public TenantFederationInfo mapRow(ResultSet resultSet, int rowNum) throws SQLException {
            return new TenantFederationInfo(
                    resultSet.getLong("id"),
                    resultSet.getString("tenant_code"),
                    resultSet.getString("issuer"),
                    resultSet.getString("external_tenant"),
                    resultSet.getInt("status")
            );
        }
    };

    private final JdbcTemplate jdbcTemplate;

    public IamJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 按用户名查询用户。
     */
    public Optional<IamUserInfo> findUserByUsername(String username) {
        String sql = """
                select id, username, password, display_name, status, mfa_enabled, risk_level
                       , tenant_code, department, last_login_ip
                from iam_user
                where username = ?
                """;
        List<IamUserInfo> users = jdbcTemplate.query(sql, USER_ROW_MAPPER, username);
        return users.stream().findFirst();
    }

    /**
     * 按用户 ID 查询用户。
     */
    public Optional<IamUserInfo> findUserById(Long userId) {
        String sql = """
                select id, username, password, display_name, status, mfa_enabled, risk_level
                       , tenant_code, department, last_login_ip
                from iam_user
                where id = ?
                """;
        List<IamUserInfo> users = jdbcTemplate.query(sql, USER_ROW_MAPPER, userId);
        return users.stream().findFirst();
    }

    /**
     * 查询用户拥有的权限编码列表。
     */
    public List<String> listPermissionCodesByUserId(Long userId) {
        String sql = """
                select distinct p.permission_code
                from iam_user_role ur
                join iam_role_permission rp on ur.role_id = rp.role_id
                join iam_permission p on rp.permission_id = p.id
                where ur.user_id = ?
                order by p.permission_code
                """;
        List<String> permissionCodes = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("permission_code"), userId);
        return permissionCodes == null ? Collections.emptyList() : permissionCodes;
    }

    /**
     * 查询用户拥有的角色编码列表。
     */
    public List<String> listRoleCodesByUserId(Long userId) {
        String sql = """
                select distinct r.role_code
                from iam_user_role ur
                join iam_role r on ur.role_id = r.id
                where ur.user_id = ?
                order by r.role_code
                """;
        List<String> roleCodes = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("role_code"), userId);
        return roleCodes == null ? Collections.emptyList() : roleCodes;
    }

    /**
     * 校验用户是否具有指定资源动作权限。
     */
    public boolean hasPermission(Long userId, String resourceCode, String actionCode) {
        String sql = """
                select count(1)
                from iam_user_role ur
                join iam_role_permission rp on ur.role_id = rp.role_id
                join iam_permission p on rp.permission_id = p.id
                where ur.user_id = ?
                  and p.resource_code = ?
                  and p.action_code = ?
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, resourceCode, actionCode);
        return count != null && count > 0;
    }

    /**
     * 查询用户角色绑定的策略。
     */
    public List<PolicyRuleInfo> listActivePoliciesByUserId(Long userId) {
        String sql = """
                select distinct p.id, p.policy_type, p.expression
                from iam_user_role ur
                join iam_role_policy rp on ur.role_id = rp.role_id
                join iam_policy p on rp.policy_id = p.id
                where ur.user_id = ?
                  and p.status = 1
                order by p.id
                """;
        return jdbcTemplate.query(sql, POLICY_RULE_ROW_MAPPER, userId);
    }

    /**
     * 保存刷新令牌会话。
     */
    public void saveTokenSession(Long userId, String refreshToken, LocalDateTime expiredAt) {
        String sql = """
                insert into iam_token_session (user_id, refresh_token, expired_at, is_revoked, gmt_create, gmt_modified)
                values (?, ?, ?, 0, current_timestamp, current_timestamp)
                """;
        jdbcTemplate.update(sql, userId, refreshToken, Timestamp.valueOf(expiredAt));
    }

    /**
     * 新增 MFA OTP 校验码。
     */
    public void saveMfaOtp(Long userId, String otpCode, LocalDateTime expiredAt) {
        String sql = """
                insert into iam_mfa_otp (user_id, otp_code, expired_at, used, gmt_create, gmt_modified)
                values (?, ?, ?, 0, current_timestamp, current_timestamp)
                """;
        jdbcTemplate.update(sql, userId, otpCode, Timestamp.valueOf(expiredAt));
    }

    /**
     * 校验并消费 MFA OTP。
     */
    public boolean consumeMfaOtp(Long userId, String otpCode) {
        String sql = """
                update iam_mfa_otp
                set used = 1,
                    gmt_modified = current_timestamp
                where user_id = ?
                  and otp_code = ?
                  and used = 0
                  and expired_at >= current_timestamp
                """;
        return jdbcTemplate.update(sql, userId, otpCode) > 0;
    }

    /**
     * 查询有效刷新令牌会话。
     */
    public Optional<TokenSessionInfo> findTokenSessionByRefreshToken(String refreshToken) {
        String sql = """
                select id, user_id, refresh_token, expired_at, is_revoked
                from iam_token_session
                where refresh_token = ?
                """;
        List<TokenSessionInfo> sessions = jdbcTemplate.query(sql, TOKEN_SESSION_ROW_MAPPER, refreshToken);
        return sessions.stream().findFirst();
    }

    /**
     * 吊销刷新令牌会话。
     */
    public void revokeTokenSession(String refreshToken) {
        String sql = """
                update iam_token_session
                set is_revoked = 1,
                    gmt_modified = current_timestamp
                where refresh_token = ?
                """;
        jdbcTemplate.update(sql, refreshToken);
    }

    /**
     * 开启用户 MFA。
     */
    public void updateUserMfaEnabled(Long userId, boolean enabled) {
        String sql = """
                update iam_user
                set mfa_enabled = ?,
                    gmt_modified = current_timestamp
                where id = ?
                """;
        jdbcTemplate.update(sql, enabled ? 1 : 0, userId);
    }

    /**
     * 写入审计日志。
     */
    public void saveAuditLog(
            String eventType,
            String username,
            Long userId,
            String resourceCode,
            String actionCode,
            String decision,
            String detail,
            String clientIp
    ) {
        String sql = """
                insert into iam_audit_log
                (event_type, username, user_id, resource_code, action_code, decision, detail, client_ip, gmt_create, gmt_modified)
                values (?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                """;
        jdbcTemplate.update(sql, eventType, username, userId, resourceCode, actionCode, decision, detail, clientIp);
    }

    /**
     * 新增用户并返回用户 ID。
     */
    public Long saveUser(String username, String password, String displayName) {
        String insertSql = """
                insert into iam_user (username, password, display_name, status, mfa_enabled, risk_level, gmt_create, gmt_modified)
                values (?, ?, ?, 1, 0, 'LOW', current_timestamp, current_timestamp)
                """;
        jdbcTemplate.update(insertSql, username, password, displayName);
        String querySql = "select id from iam_user where username = ?";
        return jdbcTemplate.queryForObject(querySql, Long.class, username);
    }

    /**
     * 新增角色并返回角色 ID。
     */
    public Long saveRole(String roleCode, String roleName) {
        String insertSql = """
                insert into iam_role (role_code, role_name, gmt_create, gmt_modified)
                values (?, ?, current_timestamp, current_timestamp)
                """;
        jdbcTemplate.update(insertSql, roleCode, roleName);
        String querySql = "select id from iam_role where role_code = ?";
        return jdbcTemplate.queryForObject(querySql, Long.class, roleCode);
    }

    /**
     * 覆盖角色权限关系。
     */
    public int replaceRolePermissions(Long roleId, List<String> permissionCodes) {
        String deleteSql = "delete from iam_role_permission where role_id = ?";
        jdbcTemplate.update(deleteSql, roleId);
        String insertSql = """
                insert into iam_role_permission (role_id, permission_id, gmt_create, gmt_modified)
                select ?, id, current_timestamp, current_timestamp
                from iam_permission
                where permission_code = ?
                """;
        int count = 0;
        for (String permissionCode : permissionCodes) {
            count += jdbcTemplate.update(insertSql, roleId, permissionCode);
        }
        return count;
    }

    /**
     * 新增策略并返回策略 ID。
     */
    public Long savePolicy(String policyName, String policyType, String expression) {
        String insertSql = """
                insert into iam_policy (policy_name, policy_type, expression, status, gmt_create, gmt_modified)
                values (?, ?, ?, 1, current_timestamp, current_timestamp)
                """;
        jdbcTemplate.update(insertSql, policyName, policyType, expression);
        String querySql = "select id from iam_policy where policy_name = ?";
        return jdbcTemplate.queryForObject(querySql, Long.class, policyName);
    }

    /**
     * 覆盖角色策略关系。
     */
    public int replaceRolePolicies(Long roleId, List<Long> policyIds) {
        String deleteSql = "delete from iam_role_policy where role_id = ?";
        jdbcTemplate.update(deleteSql, roleId);
        String insertSql = """
                insert into iam_role_policy (role_id, policy_id, gmt_create, gmt_modified)
                values (?, ?, current_timestamp, current_timestamp)
                """;
        int count = 0;
        for (Long policyId : policyIds) {
            count += jdbcTemplate.update(insertSql, roleId, policyId);
        }
        return count;
    }

    /**
     * 新增应用身份并返回主键 ID。
     */
    public Long saveClient(String clientId, String clientName, String clientSecret) {
        String insertSql = """
                insert into iam_client (client_id, client_name, client_secret, prev_secret, secret_rotate_at, status, gmt_create, gmt_modified)
                values (?, ?, ?, null, null, 1, current_timestamp, current_timestamp)
                """;
        jdbcTemplate.update(insertSql, clientId, clientName, clientSecret);
        String querySql = "select id from iam_client where client_id = ?";
        return jdbcTemplate.queryForObject(querySql, Long.class, clientId);
    }

    /**
     * 轮换应用密钥。
     */
    public void updateClientSecret(String clientId, String clientSecret) {
        String updateSql = """
                update iam_client
                set prev_secret = client_secret,
                    client_secret = ?,
                    secret_rotate_at = current_timestamp,
                    gmt_modified = current_timestamp
                where client_id = ?
                """;
        jdbcTemplate.update(updateSql, clientSecret, clientId);
    }

    /**
     * 按 clientId 查询客户端凭证信息。
     */
    public Optional<ClientCredentialInfo> findClientByClientId(String clientId) {
        String sql = """
                select id, client_id, client_secret, prev_secret, secret_rotate_at, status
                from iam_client
                where client_id = ?
                """;
        List<ClientCredentialInfo> clients = jdbcTemplate.query(sql, CLIENT_CREDENTIAL_ROW_MAPPER, clientId);
        return clients.stream().findFirst();
    }

    /**
     * 创建 OIDC 授权码。
     */
    public void saveOidcAuthCode(
            String authCode,
            String issuer,
            String externalSubject,
            Long userId,
            LocalDateTime expiredAt
    ) {
        String sql = """
                insert into iam_oidc_auth_code
                (auth_code, issuer, external_subject, user_id, expired_at, used, gmt_create, gmt_modified)
                values (?, ?, ?, ?, ?, 0, current_timestamp, current_timestamp)
                """;
        jdbcTemplate.update(sql, authCode, issuer, externalSubject, userId, Timestamp.valueOf(expiredAt));
    }

    /**
     * 查询有效 OIDC 授权码。
     */
    public Optional<OidcAuthCodeInfo> findOidcAuthCode(String authCode) {
        String sql = """
                select id, auth_code, issuer, external_subject, user_id, expired_at, used
                from iam_oidc_auth_code
                where auth_code = ?
                """;
        List<OidcAuthCodeInfo> authCodes = jdbcTemplate.query(sql, OIDC_AUTH_CODE_ROW_MAPPER, authCode);
        return authCodes.stream().findFirst();
    }

    /**
     * 查询启用的租户联邦映射。
     */
    public Optional<TenantFederationInfo> findActiveTenantFederation(String tenantCode, String issuer, String externalTenant) {
        String sql = """
                select id, tenant_code, issuer, external_tenant, status
                from iam_tenant_federation
                where tenant_code = ?
                  and issuer = ?
                  and external_tenant = ?
                  and status = 1
                """;
        List<TenantFederationInfo> records = jdbcTemplate.query(sql, TENANT_FEDERATION_ROW_MAPPER, tenantCode, issuer, externalTenant);
        return records.stream().findFirst();
    }

    /**
     * 查询所有租户联邦映射。
     */
    public List<TenantFederationInfo> listTenantFederations() {
        String sql = """
                select id, tenant_code, issuer, external_tenant, status
                from iam_tenant_federation
                order by id desc
                """;
        return jdbcTemplate.query(sql, TENANT_FEDERATION_ROW_MAPPER);
    }

    /**
     * 新增或更新租户联邦映射。
     */
    public void upsertTenantFederation(String tenantCode, String issuer, String externalTenant, boolean enabled) {
        String updateSql = """
                update iam_tenant_federation
                set status = ?,
                    gmt_modified = current_timestamp
                where tenant_code = ?
                  and issuer = ?
                  and external_tenant = ?
                """;
        int affected = jdbcTemplate.update(updateSql, enabled ? 1 : 0, tenantCode, issuer, externalTenant);
        if (affected > 0) {
            return;
        }
        String insertSql = """
                insert into iam_tenant_federation
                (tenant_code, issuer, external_tenant, status, gmt_create, gmt_modified)
                values (?, ?, ?, ?, current_timestamp, current_timestamp)
                """;
        jdbcTemplate.update(insertSql, tenantCode, issuer, externalTenant, enabled ? 1 : 0);
    }

    /**
     * 消费 OIDC 授权码。
     */
    public void markOidcAuthCodeUsed(Long id) {
        String sql = """
                update iam_oidc_auth_code
                set used = 1,
                    gmt_modified = current_timestamp
                where id = ?
                """;
        jdbcTemplate.update(sql, id);
    }

    /**
     * 按外部身份查询绑定用户。
     */
    public Optional<Long> findUserIdByOidcIdentity(String issuer, String externalSubject) {
        String sql = """
                select user_id
                from iam_oidc_identity
                where issuer = ?
                  and external_subject = ?
                """;
        List<Long> userIds = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong("user_id"), issuer, externalSubject);
        return userIds.stream().findFirst();
    }

    /**
     * 绑定外部身份。
     */
    public void saveOidcIdentity(String issuer, String externalSubject, Long userId) {
        String sql = """
                insert into iam_oidc_identity (issuer, external_subject, user_id, gmt_create, gmt_modified)
                values (?, ?, ?, current_timestamp, current_timestamp)
                """;
        jdbcTemplate.update(sql, issuer, externalSubject, userId);
    }

    /**
     * 查询 ABAC 策略。
     */
    public List<AbacPolicyInfo> listActiveAbacPolicies(String resourceCode, String actionCode) {
        String sql = """
                select id, policy_name, resource_code, action_code, expression, status
                       , created_by, owner, modified_by, gmt_create, gmt_modified
                from iam_abac_policy
                where status = 1
                  and resource_code = ?
                  and action_code = ?
                order by id
                """;
        return jdbcTemplate.query(sql, ABAC_POLICY_ROW_MAPPER, resourceCode, actionCode);
    }

    /**
     * 查询 ABAC 策略列表（支持筛选）。
     */
    public List<AbacPolicyInfo> listAbacPolicies(String policyName, String resourceCode, String actionCode, int limit) {
        String sql = """
                select id, policy_name, resource_code, action_code, expression, status
                       , created_by, owner, modified_by, gmt_create, gmt_modified
                from iam_abac_policy
                where status = 1
                  and (? is null or policy_name like ?)
                  and (? is null or resource_code like ?)
                  and (? is null or action_code like ?)
                order by id desc
                limit ?
                """;
        String safePolicyName = toLikeKeyword(policyName);
        String safeResourceCode = toLikeKeyword(resourceCode);
        String safeActionCode = toLikeKeyword(actionCode);
        return jdbcTemplate.query(
                sql,
                ABAC_POLICY_ROW_MAPPER,
                safePolicyName,
                safePolicyName,
                safeResourceCode,
                safeResourceCode,
                safeActionCode,
                safeActionCode,
                limit
        );
    }

    /**
     * 创建 ABAC 策略。
     */
    public Long saveAbacPolicy(String policyName, String resourceCode, String actionCode, String expression) {
        String insertSql = """
                insert into iam_abac_policy
                (policy_name, resource_code, action_code, expression, status
                 , created_by, owner, modified_by, gmt_create, gmt_modified)
                values (?, ?, ?, ?, 1, ?, ?, ?, current_timestamp, current_timestamp)
                """;
        jdbcTemplate.update(insertSql, policyName, resourceCode, actionCode, expression, "system", "system", "system");
        String querySql = "select id from iam_abac_policy where policy_name = ? order by id desc limit 1";
        return jdbcTemplate.queryForObject(querySql, Long.class, policyName);
    }

    /**
     * 创建 ABAC 策略（携带创建与责任信息）。
     */
    public Long saveAbacPolicy(
            String policyName,
            String resourceCode,
            String actionCode,
            String expression,
            String createdBy,
            String owner,
            String modifiedBy
    ) {
        String insertSql = """
                insert into iam_abac_policy
                (policy_name, resource_code, action_code, expression, status
                 , created_by, owner, modified_by, gmt_create, gmt_modified)
                values (?, ?, ?, ?, 1, ?, ?, ?, current_timestamp, current_timestamp)
                """;
        jdbcTemplate.update(insertSql, policyName, resourceCode, actionCode, expression, createdBy, owner, modifiedBy);
        String querySql = "select id from iam_abac_policy where policy_name = ? order by id desc limit 1";
        return jdbcTemplate.queryForObject(querySql, Long.class, policyName);
    }

    /**
     * 查询租户主数据。
     */
    public List<TenantInfo> listTenants(String keyword) {
        String sql = """
                select id, tenant_code, tenant_name, status
                from iam_tenant
                where status = 1
                  and (? is null or tenant_code like ? or tenant_name like ?)
                order by id
                """;
        String safeKeyword = toLikeKeyword(keyword);
        return jdbcTemplate.query(sql, TENANT_ROW_MAPPER, safeKeyword, safeKeyword, safeKeyword);
    }

    /**
     * 创建权限变更申请单。
     */
    public Long savePermissionChangeRequest(Long roleId, String permissionCodes, String requestedBy) {
        String sql = """
                insert into iam_permission_change_request
                (role_id, permission_codes, status, requested_by, gmt_create, gmt_modified)
                values (?, ?, 'PENDING', ?, current_timestamp, current_timestamp)
                """;
        jdbcTemplate.update(sql, roleId, permissionCodes, requestedBy);
        String querySql = """
                select id
                from iam_permission_change_request
                where role_id = ?
                  and requested_by = ?
                order by id desc
                limit 1
                """;
        return jdbcTemplate.queryForObject(querySql, Long.class, roleId, requestedBy);
    }

    /**
     * 查询权限变更申请单。
     */
    public Optional<PermissionChangeRequestInfo> findPermissionChangeRequest(Long requestId) {
        String sql = """
                select id, role_id, permission_codes, status, requested_by
                from iam_permission_change_request
                where id = ?
                """;
        List<PermissionChangeRequestInfo> requests = jdbcTemplate.query(sql, PERMISSION_CHANGE_REQUEST_ROW_MAPPER, requestId);
        return requests.stream().findFirst();
    }

    /**
     * 审批通过权限变更申请单。
     */
    public void approvePermissionChangeRequest(Long requestId, String approvedBy, String approvalComment) {
        String sql = """
                update iam_permission_change_request
                set status = 'APPROVED',
                    approved_by = ?,
                    approved_at = current_timestamp,
                    approval_comment = ?,
                    gmt_modified = current_timestamp
                where id = ?
                """;
        jdbcTemplate.update(sql, approvedBy, approvalComment, requestId);
    }

    /**
     * 更新用户最近登录信息。
     */
    public void updateUserLastLogin(Long userId, String clientIp) {
        String sql = """
                update iam_user
                set last_login_ip = ?,
                    last_login_at = current_timestamp,
                    gmt_modified = current_timestamp
                where id = ?
                """;
        jdbcTemplate.update(sql, clientIp, userId);
    }

    /**
     * 更新客户端最近使用时间。
     */
    public void updateClientLastUsedAt(String clientId) {
        String sql = """
                update iam_client
                set last_used_at = current_timestamp,
                    gmt_modified = current_timestamp
                where client_id = ?
                """;
        jdbcTemplate.update(sql, clientId);
    }

    /**
     * 统计用户最近拒绝鉴权次数。
     */
    public int countRecentDeniedAuthorize(Long userId, int minutes) {
        String sql = """
                select count(1)
                from iam_audit_log
                where event_type = 'AUTHORIZE_CHECK'
                  and decision = 'DENY'
                  and user_id = ?
                  and gmt_create >= dateadd('MINUTE', ?, current_timestamp)
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, -minutes);
        return count == null ? 0 : count;
    }

    /**
     * 写入风险事件。
     */
    public void saveRiskEvent(String eventType, Long userId, String tenantCode, String severity, String detail) {
        String sql = """
                insert into iam_risk_event
                (event_type, user_id, tenant_code, severity, detail, status, gmt_create, gmt_modified)
                values (?, ?, ?, ?, ?, 'OPEN', current_timestamp, current_timestamp)
                """;
        jdbcTemplate.update(sql, eventType, userId, tenantCode, severity, detail);
    }

    /**
     * 查询风险事件。
     */
    public List<RiskEventInfo> listRiskEvents(int limit) {
        String sql = """
                select id, event_type, user_id, tenant_code, severity, detail, status, gmt_create
                from iam_risk_event
                order by id desc
                limit ?
                """;
        return jdbcTemplate.query(sql, RISK_EVENT_ROW_MAPPER, limit);
    }

    /**
     * 巡检：过权账户（权限数超过阈值）。
     */
    public List<Map<String, Object>> inspectOverPrivilegedUsers(int permissionThreshold) {
        String sql = """
                select u.id as user_id, u.username, u.tenant_code, count(distinct p.permission_code) as permission_count
                from iam_user u
                join iam_user_role ur on u.id = ur.user_id
                join iam_role_permission rp on ur.role_id = rp.role_id
                join iam_permission p on rp.permission_id = p.id
                group by u.id, u.username, u.tenant_code
                having count(distinct p.permission_code) > ?
                order by permission_count desc
                """;
        return jdbcTemplate.queryForList(sql, permissionThreshold);
    }

    /**
     * 巡检：僵尸账号（长期未登录）。
     */
    public List<Map<String, Object>> inspectZombieAccounts(int inactiveDays) {
        String sql = """
                select id as user_id, username, tenant_code, last_login_at
                from iam_user
                where last_login_at is null
                   or last_login_at < dateadd('DAY', ?, current_timestamp)
                order by last_login_at
                """;
        return jdbcTemplate.queryForList(sql, -inactiveDays);
    }

    /**
     * 巡检：长期未使用客户端凭证。
     */
    public List<Map<String, Object>> inspectStaleClients(int staleDays) {
        String sql = """
                select client_id, client_name, status, last_used_at, secret_rotate_at
                from iam_client
                where last_used_at is null
                   or last_used_at < dateadd('DAY', ?, current_timestamp)
                order by last_used_at
                """;
        return jdbcTemplate.queryForList(sql, -staleDays);
    }

    private String toLikeKeyword(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return "%" + value.trim() + "%";
    }
}
