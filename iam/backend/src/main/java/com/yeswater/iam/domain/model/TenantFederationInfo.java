package com.yeswater.iam.domain.model;

import java.time.LocalDateTime;

public record TenantFederationInfo(
        Long id,
        String tenantCode,
        String issuer,
        String externalTenant,
        String appCode,
        Integer status,
        String modifiedBy,
        LocalDateTime gmtCreate,
        LocalDateTime gmtModified
) {
}
