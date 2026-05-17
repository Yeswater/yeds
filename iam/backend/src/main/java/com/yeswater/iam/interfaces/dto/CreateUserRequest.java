package com.yeswater.iam.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
        @NotBlank(message = "username不能为空") String username,
        @NotBlank(message = "password不能为空") String password,
        @NotBlank(message = "displayName不能为空") String displayName
) {
}
