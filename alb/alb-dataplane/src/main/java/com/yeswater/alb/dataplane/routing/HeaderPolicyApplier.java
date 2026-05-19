package com.yeswater.alb.dataplane.routing;

import com.yeswater.alb.dataplane.snapshot.RoutingSnapshot;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class HeaderPolicyApplier {

    /**
     * 对请求头或响应头应用策略。
     */
    public void apply(HttpHeaders headers, List<RoutingSnapshot.SnapshotHeaderPolicy> policies, String direction) {
        if (policies == null || policies.isEmpty()) {
            return;
        }
        policies.stream()
                .filter(policy -> direction.equalsIgnoreCase(policy.direction()))
                .sorted(Comparator.comparingInt(RoutingSnapshot.SnapshotHeaderPolicy::sortOrder))
                .forEach(policy -> applyOne(headers, policy));
    }

    private void applyOne(HttpHeaders headers, RoutingSnapshot.SnapshotHeaderPolicy policy) {
        String key = policy.headerKey();
        if (key == null || key.isBlank()) {
            return;
        }
        switch (policy.op().toLowerCase()) {
            case "add" -> {
                String existing = headers.getFirst(key);
                if (existing == null || existing.isBlank()) {
                    headers.set(key, policy.headerValue());
                } else if (policy.headerValue() != null && !policy.headerValue().isBlank()) {
                    headers.set(key, existing + ", " + policy.headerValue());
                }
            }
            case "set" -> headers.set(key, policy.headerValue());
            case "remove" -> headers.remove(key);
            default -> {
                // ignore unknown op
            }
        }
    }
}
