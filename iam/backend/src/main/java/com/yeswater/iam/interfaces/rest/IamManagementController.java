package com.yeswater.iam.interfaces.rest;

import com.yeswater.iam.application.IamManagementApplicationService;
import com.yeswater.iam.interfaces.dto.BindRolePermissionsRequest;
import com.yeswater.iam.interfaces.dto.BindRolePoliciesRequest;
import com.yeswater.iam.interfaces.dto.ClientAuthenticateRequest;
import com.yeswater.iam.interfaces.dto.CreateAbacPolicyRequest;
import com.yeswater.iam.interfaces.dto.CreateClientRequest;
import com.yeswater.iam.interfaces.dto.CreatePermissionChangeRequest;
import com.yeswater.iam.interfaces.dto.CreatePolicyRequest;
import com.yeswater.iam.interfaces.dto.CreateRoleRequest;
import com.yeswater.iam.interfaces.dto.CreateUserRequest;
import com.yeswater.iam.interfaces.dto.MfaEnableRequest;
import com.yeswater.iam.interfaces.dto.ApprovePermissionChangeRequest;
import com.yeswater.iam.interfaces.dto.RotateClientSecretRequest;
import com.yeswater.iam.interfaces.dto.UpsertTenantFederationRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/iam")
public class IamManagementController {

    private final IamManagementApplicationService iamManagementApplicationService;

    public IamManagementController(IamManagementApplicationService iamManagementApplicationService) {
        this.iamManagementApplicationService = iamManagementApplicationService;
    }

    /**
     * 创建用户。
     */
    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> saveUser(
            @Valid @RequestBody CreateUserRequest request,
            HttpServletRequest httpServletRequest
    ) {
        Long userId = iamManagementApplicationService.saveUser(
                request.username(),
                request.password(),
                request.displayName(),
                resolveClientIp(httpServletRequest)
        );
        return ResponseEntity.ok(Map.of("userId", userId));
    }

    /**
     * 配置用户 MFA 开关。
     */
    @PostMapping("/users/{username}/mfa")
    public ResponseEntity<Map<String, Object>> updateUserMfa(
            @PathVariable("username") String username,
            @Valid @RequestBody MfaEnableRequest request,
            HttpServletRequest httpServletRequest
    ) {
        iamManagementApplicationService.updateMfaEnabled(
                username,
                request.enabled(),
                resolveClientIp(httpServletRequest)
        );
        return ResponseEntity.ok(Map.of("message", "mfa updated"));
    }

    /**
     * 创建角色。
     */
    @PostMapping("/roles")
    public ResponseEntity<Map<String, Object>> saveRole(
            @Valid @RequestBody CreateRoleRequest request,
            HttpServletRequest httpServletRequest
    ) {
        Long roleId = iamManagementApplicationService.saveRole(
                request.roleCode(),
                request.roleName(),
                resolveClientIp(httpServletRequest)
        );
        return ResponseEntity.ok(Map.of("roleId", roleId));
    }

    /**
     * 绑定角色权限。
     */
    @PostMapping("/roles/{roleId}/permissions")
    public ResponseEntity<Map<String, Object>> saveRolePermissions(
            @PathVariable("roleId") Long roleId,
            @Valid @RequestBody BindRolePermissionsRequest request,
            HttpServletRequest httpServletRequest
    ) {
        int boundCount = iamManagementApplicationService.saveRolePermissions(
                roleId,
                request.permissionCodes(),
                resolveClientIp(httpServletRequest)
        );
        return ResponseEntity.ok(Map.of("boundCount", boundCount));
    }

    /**
     * 绑定角色策略。
     */
    @PostMapping("/roles/{roleId}/policies")
    public ResponseEntity<Map<String, Object>> saveRolePolicies(
            @PathVariable("roleId") Long roleId,
            @Valid @RequestBody BindRolePoliciesRequest request,
            HttpServletRequest httpServletRequest
    ) {
        int boundCount = iamManagementApplicationService.saveRolePolicies(
                roleId,
                request.policyIds(),
                resolveClientIp(httpServletRequest)
        );
        return ResponseEntity.ok(Map.of("boundCount", boundCount));
    }

    /**
     * 创建策略。
     */
    @PostMapping("/policies")
    public ResponseEntity<Map<String, Object>> savePolicy(
            @Valid @RequestBody CreatePolicyRequest request,
            HttpServletRequest httpServletRequest
    ) {
        Long policyId = iamManagementApplicationService.savePolicy(
                request.policyName(),
                request.policyType(),
                request.expression(),
                resolveClientIp(httpServletRequest)
        );
        return ResponseEntity.ok(Map.of("policyId", policyId));
    }

    /**
     * 创建 ABAC 策略。
     */
    @PostMapping("/abac-policies")
    public ResponseEntity<Map<String, Object>> saveAbacPolicy(
            @Valid @RequestBody CreateAbacPolicyRequest request,
            HttpServletRequest httpServletRequest
    ) {
        Long policyId = iamManagementApplicationService.saveAbacPolicy(
                request.policyName(),
                request.resource(),
                request.action(),
                request.expression(),
                resolveClientIp(httpServletRequest)
        );
        return ResponseEntity.ok(Map.of("policyId", policyId));
    }

    /**
     * 创建应用客户端。
     */
    @PostMapping("/clients")
    public ResponseEntity<Map<String, Object>> saveClient(
            @Valid @RequestBody CreateClientRequest request,
            HttpServletRequest httpServletRequest
    ) {
        Long recordId = iamManagementApplicationService.saveClient(
                request.clientId(),
                request.clientName(),
                request.clientSecret(),
                resolveClientIp(httpServletRequest)
        );
        return ResponseEntity.ok(Map.of("id", recordId));
    }

    /**
     * 客户端密钥鉴权。
     */
    @PostMapping("/clients/authenticate")
    public ResponseEntity<Map<String, Object>> authenticateClient(
            @Valid @RequestBody ClientAuthenticateRequest request,
            HttpServletRequest httpServletRequest
    ) {
        boolean authenticated = iamManagementApplicationService.authenticateClient(
                request.clientId(),
                request.clientSecret(),
                resolveClientIp(httpServletRequest)
        );
        return ResponseEntity.ok(Map.of("authenticated", authenticated));
    }

    /**
     * 轮换应用客户端密钥。
     */
    @PostMapping("/clients/{clientId}/rotate-secret")
    public ResponseEntity<Map<String, Object>> rotateClientSecret(
            @PathVariable("clientId") String clientId,
            @Valid @RequestBody RotateClientSecretRequest request,
            HttpServletRequest httpServletRequest
    ) {
        iamManagementApplicationService.rotateClientSecret(
                clientId,
                request.clientSecret(),
                resolveClientIp(httpServletRequest)
        );
        return ResponseEntity.ok(Map.of("message", "rotate success"));
    }

    /**
     * 创建权限变更申请。
     */
    @PostMapping("/permission-change-requests")
    public ResponseEntity<Map<String, Object>> createPermissionChangeRequest(
            @Valid @RequestBody CreatePermissionChangeRequest request,
            HttpServletRequest httpServletRequest
    ) {
        Long requestId = iamManagementApplicationService.createPermissionChangeRequest(
                request.roleId(),
                request.permissionCodes(),
                request.requestedBy(),
                resolveClientIp(httpServletRequest)
        );
        return ResponseEntity.ok(Map.of("requestId", requestId));
    }

    /**
     * 审批权限变更申请。
     */
    @PostMapping("/permission-change-requests/{requestId}/approve")
    public ResponseEntity<Map<String, Object>> approvePermissionChangeRequest(
            @PathVariable("requestId") Long requestId,
            @Valid @RequestBody ApprovePermissionChangeRequest request,
            HttpServletRequest httpServletRequest
    ) {
        iamManagementApplicationService.approvePermissionChangeRequest(
                requestId,
                request.approvedBy(),
                request.approvalComment(),
                resolveClientIp(httpServletRequest)
        );
        return ResponseEntity.ok(Map.of("message", "approved"));
    }

    /**
     * 配置租户联邦映射。
     */
    @PostMapping("/tenant-federations")
    public ResponseEntity<Map<String, Object>> upsertTenantFederation(
            @Valid @RequestBody UpsertTenantFederationRequest request,
            HttpServletRequest httpServletRequest
    ) {
        iamManagementApplicationService.upsertTenantFederation(
                request.tenantCode(),
                request.issuer(),
                request.externalTenant(),
                request.enabled() == null || request.enabled(),
                resolveClientIp(httpServletRequest)
        );
        return ResponseEntity.ok(Map.of("message", "tenant federation updated"));
    }

    /**
     * 查询租户联邦映射。
     */
    @GetMapping("/tenant-federations")
    public ResponseEntity<List<?>> listTenantFederations() {
        return ResponseEntity.ok(iamManagementApplicationService.listTenantFederations());
    }

    /**
     * 查询风险事件。
     */
    @GetMapping("/governance/risk-events")
    public ResponseEntity<List<?>> listRiskEvents(@RequestParam(value = "limit", defaultValue = "50") int limit) {
        return ResponseEntity.ok(iamManagementApplicationService.listRiskEvents(limit));
    }

    /**
     * 巡检：过权账户。
     */
    @GetMapping("/governance/inspections/over-privileged")
    public ResponseEntity<List<Map<String, Object>>> inspectOverPrivilegedUsers(
            @RequestParam(value = "permissionThreshold", defaultValue = "10") int permissionThreshold
    ) {
        return ResponseEntity.ok(iamManagementApplicationService.inspectOverPrivilegedUsers(permissionThreshold));
    }

    /**
     * 巡检：僵尸账号。
     */
    @GetMapping("/governance/inspections/zombie-accounts")
    public ResponseEntity<List<Map<String, Object>>> inspectZombieAccounts(
            @RequestParam(value = "inactiveDays", defaultValue = "90") int inactiveDays
    ) {
        return ResponseEntity.ok(iamManagementApplicationService.inspectZombieAccounts(inactiveDays));
    }

    /**
     * 巡检：长期未使用凭证。
     */
    @GetMapping("/governance/inspections/stale-clients")
    public ResponseEntity<List<Map<String, Object>>> inspectStaleClients(
            @RequestParam(value = "staleDays", defaultValue = "90") int staleDays
    ) {
        return ResponseEntity.ok(iamManagementApplicationService.inspectStaleClients(staleDays));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
