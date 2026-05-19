package com.yeswater.alb.controlplane.domain;

import java.time.Instant;

public record Upstream(
        Long id,
        String name,
        String targetUrl,
        boolean websocketEnabled,
        String remark,
        Instant gmtCreate,
        Instant gmtModified
) {
}
