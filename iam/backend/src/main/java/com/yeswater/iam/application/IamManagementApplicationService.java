package com.yeswater.iam.application;

import com.yeswater.iam.config.SecurityProperties;
import com.yeswater.iam.domain.exception.BusinessException;
import com.yeswater.iam.domain.model.ClientCredentialInfo;
import com.yeswater.iam.domain.model.PermissionChangeRequestInfo;
import com.yeswater.iam.domain.model.AbacPolicyInfo;
import com.yeswater.iam.domain.model.RiskEventInfo;
import com.yeswater.iam.domain.model.TenantFederationInfo;
import com.yeswater.iam.domain.model.TenantInfo;
import com.yeswater.iam.infrastructure.repository.IamJdbcRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class IamManagementApplicationService {

    private final IamJdbcRepository iamJdbcRepository;

    private final SecurityProperties securityProperties;

    public IamManagementApplicationService(IamJdbcRepository iamJdbcRepository, SecurityProperties securityProperties) {
        this.iamJdbcRepository = iamJdbcRepository;
        this.securityProperties = securityProperties;
    }

    /**
     * 创建用户。
     */
    public Long saveUser(String username, String password, String displayName, String clientIp) {
        Long userId = iamJdbcRepository.saveUser(username, password, displayName);
        iamJdbcRepository.saveAuditLog("USER_CREATE", username, userId, null, null, "ALLOW", "创建用户", clientIp);
        return userId;
    }

    /**
     * 配置用户 MFA 开关。
     */
    public void updateMfaEnabled(String username, boolean enabled, String clientIp) {
        Long userId = iamJdbcRepository.findUserByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在"))
                .id();
        iamJdbcRepository.updateUserMfaEnabled(userId, enabled);
        iamJdbcRepository.saveAuditLog(
                "USER_MFA_UPDATE",
                username,
                userId,
                null,
                null,
                "ALLOW",
                "MFA开关更新:" + enabled,
                clientIp
        );
    }

    /**
     * 创建角色。
     */
    public Long saveRole(String roleCode, String roleName, String clientIp) {
        Long roleId = iamJdbcRepository.saveRole(roleCode, roleName);
        iamJdbcRepository.saveAuditLog("ROLE_CREATE", null, null, "iam:role", "create", "ALLOW", "创建角色:" + roleCode, clientIp);
        return roleId;
    }

    /**
     * 配置角色权限。
     */
    @Transactional(rollbackFor = Exception.class)
    public int saveRolePermissions(Long roleId, List<String> permissionCodes, String clientIp) {
        int boundCount = iamJdbcRepository.replaceRolePermissions(roleId, permissionCodes);
        iamJdbcRepository.saveAuditLog(
                "ROLE_PERMISSION_BIND",
                null,
                null,
                "iam:role",
                "bind_permission",
                "ALLOW",
                "角色权限绑定, roleId=" + roleId,
                clientIp
        );
        return boundCount;
    }

    /**
     * 配置角色条件策略。
     */
    @Transactional(rollbackFor = Exception.class)
    public int saveRolePolicies(Long roleId, List<Long> policyIds, String clientIp) {
        int boundCount = iamJdbcRepository.replaceRolePolicies(roleId, policyIds);
        iamJdbcRepository.saveAuditLog(
                "ROLE_POLICY_BIND",
                null,
                null,
                "iam:role",
                "bind_policy",
                "ALLOW",
                "角色策略绑定, roleId=" + roleId,
                clientIp
        );
        return boundCount;
    }

    /**
     * 创建策略。
     */
    public Long savePolicy(String policyName, String policyType, String expression, String clientIp) {
        Long policyId = iamJdbcRepository.savePolicy(policyName, policyType, expression);
        iamJdbcRepository.saveAuditLog("POLICY_CREATE", null, null, "iam:policy", "create", "ALLOW", "创建策略:" + policyName, clientIp);
        return policyId;
    }

    /**
     * 创建 ABAC 策略。
     */
    public Long saveAbacPolicy(
            String policyName,
            String resourceCode,
            String actionCode,
            String expression,
            String createdBy,
            String owner,
            String modifiedBy,
            String clientIp
    ) {
        Long policyId = iamJdbcRepository.saveAbacPolicy(
                policyName,
                resourceCode,
                actionCode,
                expression,
                safeOperator(createdBy),
                safeOperator(owner),
                safeOperator(modifiedBy)
        );
        iamJdbcRepository.saveAuditLog(
                "ABAC_POLICY_CREATE",
                null,
                null,
                resourceCode,
                actionCode,
                "ALLOW",
                "创建ABAC策略:" + policyName,
                clientIp
        );
        return policyId;
    }

    /**
     * 查询 ABAC 策略列表。
     */
    public List<AbacPolicyInfo> listAbacPolicies(String policyName, String resourceCode, String actionCode, int limit) {
        int safeLimit = limit <= 0 ? 50 : Math.min(limit, 200);
        return iamJdbcRepository.listAbacPolicies(policyName, resourceCode, actionCode, safeLimit);
    }

    /**
     * 查询租户主数据。
     */
    public List<TenantInfo> listTenants(String keyword) {
        return iamJdbcRepository.listTenants(keyword);
    }

    /**
     * 创建应用身份。
     */
    public Long saveClient(String clientId, String clientName, String clientSecret, String clientIp) {
        Long recordId = iamJdbcRepository.saveClient(clientId, clientName, clientSecret);
        iamJdbcRepository.saveAuditLog("CLIENT_CREATE", null, null, "iam:client", "create", "ALLOW", "创建客户端:" + clientId, clientIp);
        return recordId;
    }

    /**
     * 轮换应用密钥。
     */
    public void rotateClientSecret(String clientId, String clientSecret, String clientIp) {
        iamJdbcRepository.updateClientSecret(clientId, clientSecret);
        iamJdbcRepository.saveAuditLog("CLIENT_ROTATE_SECRET", null, null, "iam:client", "rotate_secret", "ALLOW", "轮换客户端密钥", clientIp);
    }

    /**
     * 校验客户端密钥（支持轮换过渡期双密钥）。
     */
    public boolean authenticateClient(String clientId, String clientSecret, String clientIp) {
        ClientCredentialInfo client = iamJdbcRepository.findClientByClientId(clientId)
                .orElseThrow(() -> new BusinessException("客户端不存在"));
        boolean currentMatch = clientSecret.equals(client.clientSecret());
        boolean graceMatch = false;
        if (!currentMatch && client.prevSecret() != null && client.secretRotateAt() != null) {
            LocalDateTime graceDeadline = client.secretRotateAt().plusSeconds(securityProperties.getClientSecretGraceSeconds());
            graceMatch = LocalDateTime.now().isBefore(graceDeadline) && clientSecret.equals(client.prevSecret());
        }
        boolean passed = client.status() != null && client.status() == 1 && (currentMatch || graceMatch);
        iamJdbcRepository.saveAuditLog(
                "CLIENT_AUTHENTICATE",
                null,
                null,
                "iam:client",
                "authenticate",
                passed ? "ALLOW" : "DENY",
                "客户端鉴权:" + clientId,
                clientIp
        );
        if (passed) {
            iamJdbcRepository.updateClientLastUsedAt(clientId);
        }
        return passed;
    }

    /**
     * 创建权限变更申请单。
     */
    public Long createPermissionChangeRequest(Long roleId, List<String> permissionCodes, String requestedBy, String clientIp) {
        String codes = String.join(",", permissionCodes);
        Long requestId = iamJdbcRepository.savePermissionChangeRequest(roleId, codes, requestedBy);
        iamJdbcRepository.saveAuditLog(
                "PERMISSION_CHANGE_REQUEST",
                requestedBy,
                null,
                "iam:permission-change",
                "request",
                "ALLOW",
                "提交权限变更申请, requestId=" + requestId,
                clientIp
        );
        return requestId;
    }

    /**
     * 审批权限变更申请并生效。
     */
    @Transactional(rollbackFor = Exception.class)
    public void approvePermissionChangeRequest(Long requestId, String approvedBy, String approvalComment, String clientIp) {
        PermissionChangeRequestInfo requestInfo = iamJdbcRepository.findPermissionChangeRequest(requestId)
                .orElseThrow(() -> new BusinessException("权限变更申请不存在"));
        if (!"PENDING".equalsIgnoreCase(requestInfo.status())) {
            throw new BusinessException("权限变更申请状态不可审批");
        }
        List<String> permissionCodes = Arrays.stream(requestInfo.permissionCodes().split(","))
                .map(String::trim)
                .filter(code -> !code.isBlank())
                .toList();
        iamJdbcRepository.replaceRolePermissions(requestInfo.roleId(), permissionCodes);
        iamJdbcRepository.approvePermissionChangeRequest(requestId, approvedBy, approvalComment);
        iamJdbcRepository.saveAuditLog(
                "PERMISSION_CHANGE_APPROVE",
                approvedBy,
                null,
                "iam:permission-change",
                "approve",
                "ALLOW",
                "审批权限变更并生效, requestId=" + requestId,
                clientIp
        );
    }

    /**
     * 配置租户联邦映射。
     */
    public void upsertTenantFederation(String tenantCode, String issuer, String externalTenant, boolean enabled, String clientIp) {
        iamJdbcRepository.upsertTenantFederation(tenantCode, issuer, externalTenant, enabled);
        iamJdbcRepository.saveAuditLog(
                "TENANT_FEDERATION_UPSERT",
                null,
                null,
                "iam:tenant-federation",
                "upsert",
                "ALLOW",
                "租户联邦映射更新:" + tenantCode + "/" + issuer + "/" + externalTenant,
                clientIp
        );
    }

    /**
     * 查询租户联邦映射。
     */
    public List<TenantFederationInfo> listTenantFederations() {
        return iamJdbcRepository.listTenantFederations();
    }

    /**
     * 查询风险事件。
     */
    public List<RiskEventInfo> listRiskEvents(int limit) {
        int safeLimit = limit <= 0 ? 50 : Math.min(limit, 200);
        return iamJdbcRepository.listRiskEvents(safeLimit);
    }

    /**
     * 巡检：过权账户。
     */
    public List<Map<String, Object>> inspectOverPrivilegedUsers(int permissionThreshold) {
        int threshold = permissionThreshold <= 0 ? 10 : permissionThreshold;
        return iamJdbcRepository.inspectOverPrivilegedUsers(threshold);
    }

    /**
     * 巡检：僵尸账号。
     */
    public List<Map<String, Object>> inspectZombieAccounts(int inactiveDays) {
        int days = inactiveDays <= 0 ? 90 : inactiveDays;
        return iamJdbcRepository.inspectZombieAccounts(days);
    }

    /**
     * 巡检：长期未使用凭证。
     */
    public List<Map<String, Object>> inspectStaleClients(int staleDays) {
        int days = staleDays <= 0 ? 90 : staleDays;
        return iamJdbcRepository.inspectStaleClients(days);
    }

    private String safeOperator(String value) {
        if (value == null || value.isBlank()) {
            return "system";
        }
        return value.trim();
    }
}
