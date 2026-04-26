package com.apig.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "apig.gateway")
public record GatewayProperties(
        String jwtSecret,
        int rateLimitPerMinute
) {
}
