package com.yeswater.iam.domain.model;

import java.time.LocalDateTime;

public record AbacPolicyInfo(
        Long id,
        String policyName,
        String resourceCode,
        String actionCode,
        String expression,
        String appCode,
        Integer status,
        String createdBy,
        String owner,
        String modifiedBy,
        LocalDateTime gmtCreate,
        LocalDateTime gmtModified
) {
}
