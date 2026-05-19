package com.yeswater.alb.controlplane.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

public record HeaderPolicyRequest(
        @NotBlank String direction,
        @NotBlank String op,
        @NotBlank String headerKey,
        String headerValue,
        Integer sortOrder,
        Boolean enabled
) {
}
