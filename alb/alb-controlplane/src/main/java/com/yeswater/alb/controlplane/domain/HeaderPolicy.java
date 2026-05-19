package com.yeswater.alb.controlplane.domain;

import java.time.Instant;

public record HeaderPolicy(
        Long id,
        Long routeId,
        String direction,
        String op,
        String headerKey,
        String headerValue,
        int sortOrder,
        boolean enabled,
        Instant gmtCreate,
        Instant gmtModified
) {
}
