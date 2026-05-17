package com.yeswater.iam.domain.model;

public record IamUserInfo(
        Long id,
        String username,
        String password,
        String displayName,
        Integer status,
        Integer mfaEnabled,
        String riskLevel,
        String tenantCode,
        String department,
        String lastLoginIp
) {
}
