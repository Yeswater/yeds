package com.yeswater.iam.domain.model;

public record TenantInfo(
        Long id,
        String tenantCode,
        String tenantName,
        Integer status
) {
}
