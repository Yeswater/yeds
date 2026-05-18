package com.yeswater.iam.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

public record UpsertTenantFederationRequest(
        @NotBlank(message = "tenantCode不能为空") String tenantCode,
        @NotBlank(message = "issuer不能为空") String issuer,
        @NotBlank(message = "externalTenant不能为空") String externalTenant,
        Boolean enabled,
        String modifiedBy,
        String appCode
) {
}
