package com.yeswater.iam.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

public record OidcTokenRequest(@NotBlank(message = "authorizationCode不能为空") String authorizationCode) {
}
