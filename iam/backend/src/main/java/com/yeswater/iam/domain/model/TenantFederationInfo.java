package com.yeswater.iam.domain.model;

public record TenantFederationInfo(
        Long id,
        String tenantCode,
        String issuer,
        String externalTenant,
        Integer status
) {
}
