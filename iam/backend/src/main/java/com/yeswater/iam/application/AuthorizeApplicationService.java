package com.yeswater.iam.application;

import com.yeswater.iam.infrastructure.repository.IamJdbcRepository;
import com.yeswater.iam.domain.model.AbacPolicyInfo;
import com.yeswater.iam.domain.model.PolicyRuleInfo;
import com.yeswater.iam.interfaces.dto.AuthorizeCheckRequest;
import com.yeswater.iam.interfaces.dto.AuthorizeCheckResponse;
import com.yeswater.iam.interfaces.dto.UserPermissionsResponse;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class AuthorizeApplicationService {

    private final IamJdbcRepository iamJdbcRepository;

    public AuthorizeApplicationService(IamJdbcRepository iamJdbcRepository) {
        this.iamJdbcRepository = iamJdbcRepository;
    }

    /**
     * 执行单个权限点检查。
     */
    public AuthorizeCheckResponse check(AuthorizeCheckRequest request, String clientIp) {
        boolean roleAllowed = iamJdbcRepository.hasPermission(request.userId(), request.resource(), request.action());
        boolean policyAllowed = roleAllowed && evaluatePolicies(request.userId(), clientIp, request.envTag());
        boolean abacAllowed = roleAllowed && policyAllowed && evaluateAbacPolicies(request);
        boolean allowed = roleAllowed && policyAllowed && abacAllowed;
        String decision = allowed ? "ALLOW" : "DENY";
        String detail;
        if (!roleAllowed) {
            detail = "RBAC权限拒绝";
        } else if (!policyAllowed) {
            detail = "条件策略拒绝";
        } else if (!abacAllowed) {
            detail = "ABAC策略拒绝";
        } else {
            detail = "权限检查通过";
        }
        iamJdbcRepository.findUserById(request.userId()).ifPresent(user ->
                iamJdbcRepository.saveAuditLog(
                        "AUTHORIZE_CHECK",
                        user.username(),
                        user.id(),
                        request.resource(),
                        request.action(),
                        decision,
                        detail,
                        clientIp
                )
        );
        if (!allowed) {
            if (!roleAllowed) {
                iamJdbcRepository.findUserById(request.userId()).ifPresent(user -> iamJdbcRepository.saveRiskEvent(
                        "UNAUTHORIZED_ACCESS",
                        user.id(),
                        user.tenantCode(),
                        "HIGH",
                        "越权访问尝试: " + request.resource() + ":" + request.action()
                ));
            }
            detectHighFrequencyDeny(request.userId());
        }
        return new AuthorizeCheckResponse(request.userId(), request.resource(), request.action(), allowed);
    }

    /**
     * 查询用户所有权限编码。
     */
    public UserPermissionsResponse listUserPermissions(Long userId) {
        List<String> permissions = iamJdbcRepository.listPermissionCodesByUserId(userId);
        return new UserPermissionsResponse(userId, permissions);
    }

    private void detectHighFrequencyDeny(Long userId) {
        int denyCount = iamJdbcRepository.countRecentDeniedAuthorize(userId, 5);
        if (denyCount >= 5) {
            iamJdbcRepository.findUserById(userId).ifPresent(user -> iamJdbcRepository.saveRiskEvent(
                    "HIGH_FREQUENCY_DENY",
                    user.id(),
                    user.tenantCode(),
                    "MEDIUM",
                    "5分钟内鉴权拒绝次数达到" + denyCount
            ));
        }
    }

    private boolean evaluatePolicies(Long userId, String clientIp, String envTag) {
        List<PolicyRuleInfo> policyRules = iamJdbcRepository.listActivePoliciesByUserId(userId);
        for (PolicyRuleInfo policyRuleInfo : policyRules) {
            boolean passed = switch (policyRuleInfo.policyType()) {
                case "IP" -> matchesIpPolicy(policyRuleInfo.expression(), clientIp);
                case "TIME_WINDOW" -> matchesTimeWindowPolicy(policyRuleInfo.expression(), LocalTime.now());
                case "ENV_TAG" -> matchesEnvTagPolicy(policyRuleInfo.expression(), envTag);
                default -> true;
            };
            if (!passed) {
                return false;
            }
        }
        return true;
    }

    private boolean evaluateAbacPolicies(AuthorizeCheckRequest request) {
        List<AbacPolicyInfo> policies = iamJdbcRepository.listActiveAbacPolicies(request.resource(), request.action());
        if (policies.isEmpty()) {
            return true;
        }
        Map<String, String> attributes = request.attributes() == null ? Map.of() : request.attributes();
        return policies.stream().anyMatch(policy -> matchesAbacExpression(policy.expression(), request, attributes));
    }

    private boolean matchesAbacExpression(String expression, AuthorizeCheckRequest request, Map<String, String> attributes) {
        if (expression == null || expression.isBlank()) {
            return true;
        }
        String[] conditions = expression.split(";");
        for (String condition : conditions) {
            String trimmed = condition.trim();
            if (trimmed.isBlank() || !trimmed.contains("=")) {
                continue;
            }
            String[] pair = trimmed.split("=", 2);
            String key = pair[0].trim();
            String expected = pair[1].trim();
            String actual = resolveAbacValue(key, request, attributes);
            if (actual == null || !actual.equalsIgnoreCase(expected)) {
                return false;
            }
        }
        return true;
    }

    private String resolveAbacValue(String key, AuthorizeCheckRequest request, Map<String, String> attributes) {
        if ("envTag".equalsIgnoreCase(key)) {
            return request.envTag();
        }
        if ("tenantCode".equalsIgnoreCase(key)) {
            return request.tenantCode();
        }
        return attributes.get(key);
    }

    private boolean matchesIpPolicy(String expression, String clientIp) {
        if (expression == null || expression.isBlank() || clientIp == null || clientIp.isBlank()) {
            return false;
        }
        String[] rules = expression.split(",");
        for (String rule : rules) {
            String trimmed = rule.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (isInCidr(clientIp, trimmed)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesTimeWindowPolicy(String expression, LocalTime now) {
        if (expression == null || expression.isBlank()) {
            return true;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        String[] windows = expression.split(",");
        for (String window : windows) {
            String trimmed = window.trim();
            if (trimmed.isEmpty() || !trimmed.contains("-")) {
                continue;
            }
            String[] pair = trimmed.split("-");
            if (pair.length != 2) {
                continue;
            }
            LocalTime start = LocalTime.parse(pair[0].trim(), formatter);
            LocalTime end = LocalTime.parse(pair[1].trim(), formatter);
            if (!now.isBefore(start) && !now.isAfter(end)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesEnvTagPolicy(String expression, String envTag) {
        if (expression == null || expression.isBlank()) {
            return true;
        }
        if (envTag == null || envTag.isBlank()) {
            return false;
        }
        String[] allowedTags = expression.split(",");
        for (String allowedTag : allowedTags) {
            if (envTag.equalsIgnoreCase(allowedTag.trim())) {
                return true;
            }
        }
        return false;
    }

    private boolean isInCidr(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            if (parts.length != 2) {
                return false;
            }
            byte[] ipBytes = InetAddress.getByName(ip).getAddress();
            byte[] networkBytes = InetAddress.getByName(parts[0]).getAddress();
            int prefix = Integer.parseInt(parts[1]);
            int fullBytes = prefix / 8;
            int remainingBits = prefix % 8;
            for (int i = 0; i < fullBytes; i++) {
                if (ipBytes[i] != networkBytes[i]) {
                    return false;
                }
            }
            if (remainingBits == 0) {
                return true;
            }
            int mask = 0xFF << (8 - remainingBits);
            return (ipBytes[fullBytes] & mask) == (networkBytes[fullBytes] & mask);
        } catch (Exception ex) {
            return false;
        }
    }
}
