package com.yeswater.alb.controlplane.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "alb.controlplane")
public record AlbControlplaneProperties(
        String nginxOutputPath,
        String caddyOutputPath,
        int dataplaneListenPort
) {
}
