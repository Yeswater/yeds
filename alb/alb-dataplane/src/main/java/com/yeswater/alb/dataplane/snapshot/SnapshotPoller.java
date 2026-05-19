package com.yeswater.alb.dataplane.snapshot;

import com.yeswater.alb.dataplane.config.AlbDataplaneProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class SnapshotPoller {

    private static final Logger log = LoggerFactory.getLogger(SnapshotPoller.class);

    private final WebClient webClient;
    private final AlbDataplaneProperties properties;
    private final SnapshotStore snapshotStore;

    public SnapshotPoller(AlbDataplaneProperties properties, SnapshotStore snapshotStore) {
        this.properties = properties;
        this.snapshotStore = snapshotStore;
        this.webClient = WebClient.builder().build();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadOnStartup() {
        refresh();
    }

    @Scheduled(fixedDelayString = "${alb.dataplane.snapshot-poll-interval-ms:10000}")
    public void refresh() {
        webClient.get()
                .uri(properties.controlplaneSnapshotUrl())
                .retrieve()
                .bodyToMono(RoutingSnapshot.class)
                .doOnNext(snapshot -> {
                    snapshotStore.update(snapshot);
                    log.info("已加载路由快照 version={}, routes={}", snapshot.version(), snapshot.routes().size());
                })
                .doOnError(error -> log.warn("拉取路由快照失败: {}", error.getMessage()))
                .onErrorComplete()
                .block();
    }
}
