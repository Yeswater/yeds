package com.apig.auth.model;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
