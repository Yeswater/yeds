package com.yeswater.alb.controlplane.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RouteRequest(
        String env,
        @NotBlank String host,
        String pathPattern,
        Integer priority,
        @NotNull Long upstreamId,
        Boolean enabled,
        Boolean stripPrefix,
        String redirectUrl,
        String remark
) {
}
