package com.yeswater.alb.controlplane.interfaces.rest;

import com.yeswater.alb.controlplane.service.PublishService;
import com.yeswater.alb.controlplane.snapshot.RoutingSnapshot;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/alb/v1")
public class AlbPublishController {

    private final PublishService publishService;

    public AlbPublishController(PublishService publishService) {
        this.publishService = publishService;
    }

    /**
     * 发布路由配置。
     */
    @PostMapping("/publish")
    public Map<String, Object> publish() throws IOException {
        RoutingSnapshot snapshot = publishService.publish();
        return Map.of("version", snapshot.version(), "routeCount", snapshot.routes().size());
    }

    /**
     * 数据面拉取当前生效快照。
     */
    @GetMapping("/snapshot")
    public RoutingSnapshot snapshot() {
        return publishService.currentSnapshot();
    }

    /**
     * 预览 nginx 配置（数据面）。
     */
    @GetMapping("/publish/nginx-preview")
    public Map<String, String> nginxPreview() {
        return Map.of("content", publishService.previewNginx());
    }

    /**
     * 预览 Caddy 配置（兼容）。
     */
    @GetMapping("/publish/caddy-preview")
    public Map<String, String> caddyPreview() {
        return Map.of("content", publishService.previewCaddy());
    }
}
