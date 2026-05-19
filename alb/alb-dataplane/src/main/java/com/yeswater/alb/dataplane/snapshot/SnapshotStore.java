package com.yeswater.alb.dataplane.snapshot;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class SnapshotStore {

    private final AtomicReference<RoutingSnapshot> snapshot = new AtomicReference<>(
            new RoutingSnapshot(0, 9080, List.of()));

    public void update(RoutingSnapshot routingSnapshot) {
        snapshot.set(routingSnapshot);
    }

    public RoutingSnapshot current() {
        return snapshot.get();
    }

    public List<RoutingSnapshot.SnapshotRoute> routes() {
        RoutingSnapshot current = snapshot.get();
        return current.routes() == null ? Collections.emptyList() : current.routes();
    }
}
