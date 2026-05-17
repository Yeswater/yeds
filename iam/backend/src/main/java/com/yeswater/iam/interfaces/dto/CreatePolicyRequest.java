package com.yeswater.iam.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatePolicyRequest(
        @NotBlank(message = "policyName不能为空") String policyName,
        @NotBlank(message = "policyType不能为空") String policyType,
        @NotBlank(message = "expression不能为空") String expression
) {
}
