package com.yeswater.alb.controlplane.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

public record UpstreamRequest(
        @NotBlank String name,
        @NotBlank String targetUrl,
        Boolean websocketEnabled,
        String remark
) {
}
