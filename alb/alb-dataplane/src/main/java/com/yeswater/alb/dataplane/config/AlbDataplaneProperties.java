package com.yeswater.alb.dataplane.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "alb.dataplane")
public record AlbDataplaneProperties(
        String controlplaneSnapshotUrl,
        long snapshotPollIntervalMs
) {
}
