package com.yeswater.alb.controlplane.interfaces.rest;

import com.yeswater.alb.controlplane.domain.Route;
import com.yeswater.alb.controlplane.interfaces.dto.RouteRequest;
import com.yeswater.alb.controlplane.repository.AlbJdbcRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alb/v1/routes")
public class AlbRouteController {

    private final AlbJdbcRepository repository;

    public AlbRouteController(AlbJdbcRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Route> list() {
        return repository.listRoutes();
    }

    @GetMapping("/{id}")
    public Route get(@PathVariable Long id) {
        return repository.findRoute(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "路由不存在"));
    }

    @PostMapping
    public Map<String, Object> create(@Valid @RequestBody RouteRequest request) {
        ensureUpstream(request.upstreamId());
        long id = repository.insertRoute(
                defaultEnv(request.env()),
                request.host(),
                defaultPath(request.pathPattern()),
                defaultPriority(request.priority()),
                request.upstreamId(),
                request.enabled() == null || request.enabled(),
                request.stripPrefix() != null && request.stripPrefix(),
                request.redirectUrl(),
                request.remark());
        return Map.of("id", id);
    }

    @PutMapping("/{id}")
    public void update(@PathVariable Long id, @Valid @RequestBody RouteRequest request) {
        Route existing = repository.findRoute(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "路由不存在"));
        if (existing.systemLocked() && !existing.host().equals(request.host())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "系统路由不可修改 Host");
        }
        ensureUpstream(request.upstreamId());
        repository.updateRoute(
                id,
                defaultEnv(request.env()),
                request.host(),
                defaultPath(request.pathPattern()),
                defaultPriority(request.priority()),
                request.upstreamId(),
                request.enabled() == null || request.enabled(),
                request.stripPrefix() != null && request.stripPrefix(),
                request.redirectUrl(),
                request.remark());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        if (repository.isRouteSystemLocked(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "系统内置路由不可删除");
        }
        if (repository.deleteRoute(id) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "路由不存在或不可删除");
        }
    }

    private void ensureUpstream(Long upstreamId) {
        if (repository.findUpstream(upstreamId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "上游不存在");
        }
    }

    private String defaultEnv(String env) {
        return env == null || env.isBlank() ? "dev" : env;
    }

    private String defaultPath(String pathPattern) {
        return pathPattern == null || pathPattern.isBlank() ? "/**" : pathPattern;
    }

    private int defaultPriority(Integer priority) {
        return priority == null ? 0 : priority;
    }
}
