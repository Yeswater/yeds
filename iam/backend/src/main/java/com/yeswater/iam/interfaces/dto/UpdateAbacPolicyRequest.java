package com.yeswater.iam.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 更新 ABAC 策略请求。
 */
public record UpdateAbacPolicyRequest(
        @NotBlank(message = "policyName不能为空") String policyName,
        @NotBlank(message = "resource不能为空") String resource,
        @NotBlank(message = "action不能为空") String action,
        @NotBlank(message = "expression不能为空") String expression,
        String owner,
        String modifiedBy,
        String appCode
) {
}
