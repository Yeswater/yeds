package com.yeswater.alb.controlplane.snapshot;

import java.util.List;

public record RoutingSnapshot(
        int version,
        int dataplaneListenPort,
        List<SnapshotRoute> routes
) {

    public record SnapshotRoute(
            Long id,
            String host,
            String pathPattern,
            int priority,
            String targetUrl,
            boolean websocketEnabled,
            boolean stripPrefix,
            String redirectUrl,
            List<SnapshotHeaderPolicy> headers
    ) {
    }

    public record SnapshotHeaderPolicy(
            String direction,
            String op,
            String headerKey,
            String headerValue,
            int sortOrder
    ) {
    }
}
