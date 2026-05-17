package com.yeswater.iam.domain.model;

import java.time.LocalDateTime;

public record RiskEventInfo(
        Long id,
        String eventType,
        Long userId,
        String tenantCode,
        String severity,
        String detail,
        String status,
        LocalDateTime createdAt
) {
}
