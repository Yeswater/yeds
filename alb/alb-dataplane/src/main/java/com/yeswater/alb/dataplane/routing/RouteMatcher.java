package com.yeswater.alb.dataplane.routing;

import com.yeswater.alb.dataplane.snapshot.RoutingSnapshot;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class RouteMatcher {

    /**
     * 按 Host + Path 匹配路由。
     */
    public Optional<RoutingSnapshot.SnapshotRoute> match(String host, String path, List<RoutingSnapshot.SnapshotRoute> routes) {
        if (host == null || routes == null || routes.isEmpty()) {
            return Optional.empty();
        }
        final String normalizedHost = stripPort(host.toLowerCase(Locale.ROOT));
        final String requestPath = path == null || path.isBlank() ? "/" : path;

        return routes.stream()
                .filter(route -> normalizedHost.equals(route.host().toLowerCase(Locale.ROOT)))
                .filter(route -> pathMatches(route.pathPattern(), requestPath))
                .max(Comparator
                        .comparingInt((RoutingSnapshot.SnapshotRoute route) -> patternSpecificity(route.pathPattern()))
                        .thenComparingInt(RoutingSnapshot.SnapshotRoute::priority)
                        .thenComparingLong(RoutingSnapshot.SnapshotRoute::id));
    }

    private boolean pathMatches(String pattern, String path) {
        if (pattern == null || pattern.isBlank() || "/**".equals(pattern)) {
            return true;
        }
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.equals(prefix) || path.startsWith(prefix.endsWith("/") ? prefix : prefix + "/");
        }
        return pattern.equals(path);
    }

    private int patternSpecificity(String pattern) {
        if (pattern == null) {
            return 0;
        }
        return pattern.length();
    }

    private String stripPort(String hostValue) {
        int portIdx = hostValue.indexOf(':');
        if (portIdx > 0) {
            return hostValue.substring(0, portIdx);
        }
        return hostValue;
    }
}
