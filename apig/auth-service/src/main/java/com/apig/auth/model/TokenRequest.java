package com.apig.auth.model;

import jakarta.validation.constraints.NotBlank;

public record TokenRequest(
        @NotBlank String clientId,
        @NotBlank String clientSecret
) {
}
