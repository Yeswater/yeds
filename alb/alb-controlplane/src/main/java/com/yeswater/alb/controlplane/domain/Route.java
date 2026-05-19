package com.yeswater.alb.controlplane.domain;

import java.time.Instant;

public record Route(
        Long id,
        String env,
        String host,
        String pathPattern,
        int priority,
        Long upstreamId,
        String upstreamName,
        String upstreamTargetUrl,
        boolean enabled,
        boolean stripPrefix,
        String redirectUrl,
        boolean systemLocked,
        String remark,
        Instant gmtCreate,
        Instant gmtModified
) {
}
