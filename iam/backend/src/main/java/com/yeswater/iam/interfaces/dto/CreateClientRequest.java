package com.yeswater.iam.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateClientRequest(
        @NotBlank(message = "clientId不能为空") String clientId,
        @NotBlank(message = "clientName不能为空") String clientName,
        @NotBlank(message = "clientSecret不能为空") String clientSecret
) {
}
