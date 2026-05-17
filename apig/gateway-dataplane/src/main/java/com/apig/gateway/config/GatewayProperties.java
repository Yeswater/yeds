package com.apig.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "apig.gateway")
public record GatewayProperties(
        String iamJwkSetUri,
        String iamAuthorizeCheckUrl,
        String iamInternalAccessToken,
        String trustedHeaderToken,
        String bidsConfigBaseUrl,
        String bidsExecBaseUrl,
        String iamBaseUrl,
        int rateLimitPerMinute
) {
}
