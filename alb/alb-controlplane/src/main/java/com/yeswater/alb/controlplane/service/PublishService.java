package com.yeswater.alb.controlplane.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeswater.alb.controlplane.config.AlbControlplaneProperties;
import com.yeswater.alb.controlplane.repository.AlbJdbcRepository;
import com.yeswater.alb.controlplane.snapshot.RoutingSnapshot;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Service
public class PublishService {

    private final AlbJdbcRepository repository;
    private final NginxConfigRenderer nginxConfigRenderer;
    private final CaddyConfigRenderer caddyConfigRenderer;
    private final AlbControlplaneProperties properties;
    private final ObjectMapper objectMapper;

    private volatile RoutingSnapshot activeSnapshot;

    public PublishService(
            AlbJdbcRepository repository,
            NginxConfigRenderer nginxConfigRenderer,
            CaddyConfigRenderer caddyConfigRenderer,
            AlbControlplaneProperties properties,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.nginxConfigRenderer = nginxConfigRenderer;
        this.caddyConfigRenderer = caddyConfigRenderer;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * 发布当前路由配置为快照，并写入 nginx 配置（数据面）。
     */
    public RoutingSnapshot publish() throws IOException {
        int version = repository.nextReleaseVersion();
        RoutingSnapshot snapshot = new RoutingSnapshot(
                version,
                properties.dataplaneListenPort(),
                repository.listEnabledSnapshotRoutes());
        String snapshotJson = toJson(snapshot);
        String nginxConfig = nginxConfigRenderer.render(snapshot);
        repository.insertRelease(version, snapshotJson, nginxConfig);
        writeConfigFile(properties.nginxOutputPath(), nginxConfig);
        writeConfigFile(properties.caddyOutputPath(), caddyConfigRenderer.render(snapshot));
        activeSnapshot = snapshot;
        return snapshot;
    }

    /**
     * 返回当前生效快照；若从未发布则从数据库路由构建。
     */
    public RoutingSnapshot currentSnapshot() {
        if (activeSnapshot != null) {
            return activeSnapshot;
        }
        Optional<RoutingSnapshot> latest = repository.findLatestSnapshot(properties.dataplaneListenPort());
        if (latest.isPresent()) {
            activeSnapshot = latest.get();
            return activeSnapshot;
        }
        RoutingSnapshot bootstrap = new RoutingSnapshot(
                0,
                properties.dataplaneListenPort(),
                repository.listEnabledSnapshotRoutes());
        activeSnapshot = bootstrap;
        return bootstrap;
    }

    public String previewNginx() {
        return nginxConfigRenderer.render(buildDraftSnapshot());
    }

    public String previewCaddy() {
        return caddyConfigRenderer.render(buildDraftSnapshot());
    }

    private RoutingSnapshot buildDraftSnapshot() {
        return new RoutingSnapshot(
                0,
                properties.dataplaneListenPort(),
                repository.listEnabledSnapshotRoutes());
    }

    private void writeConfigFile(String outputPath, String content) throws IOException {
        Path path = Path.of(outputPath);
        Files.createDirectories(path.getParent() == null ? Path.of(".") : path.getParent());
        Files.writeString(path, content);
    }

    private String toJson(RoutingSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("序列化路由快照失败", ex);
        }
    }
}
