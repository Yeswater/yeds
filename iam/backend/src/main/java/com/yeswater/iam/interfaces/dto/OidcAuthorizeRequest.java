package com.yeswater.iam.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

public record OidcAuthorizeRequest(
        @NotBlank(message = "issuer不能为空") String issuer,
        @NotBlank(message = "externalSubject不能为空") String externalSubject,
        @NotBlank(message = "externalTenant不能为空") String externalTenant,
        @NotBlank(message = "username不能为空") String username
) {
}
