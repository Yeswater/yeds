package com.yeswater.iam.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

public record RotateClientSecretRequest(@NotBlank(message = "clientSecret不能为空") String clientSecret) {
}
