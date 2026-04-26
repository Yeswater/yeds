package com.apig.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "apig.auth")
public record AuthProperties(
        String jwtSecret,
        long tokenExpireSeconds
) {
}
