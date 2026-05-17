package com.yeswater.iam.domain.model;

public record AbacPolicyInfo(
        Long id,
        String policyName,
        String resourceCode,
        String actionCode,
        String expression,
        Integer status
) {
}
