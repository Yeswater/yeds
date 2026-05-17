package com.yeswater.iam.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

public record ClientAuthenticateRequest(
        @NotBlank(message = "clientId不能为空") String clientId,
        @NotBlank(message = "clientSecret不能为空") String clientSecret
) {
}
