package com.yeswater.iam.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

public record MfaSendOtpRequest(@NotBlank(message = "username不能为空") String username) {
}
