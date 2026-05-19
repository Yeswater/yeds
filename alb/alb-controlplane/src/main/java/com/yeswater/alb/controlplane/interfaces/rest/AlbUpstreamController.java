package com.yeswater.alb.controlplane.interfaces.rest;

import com.yeswater.alb.controlplane.domain.Upstream;
import com.yeswater.alb.controlplane.interfaces.dto.UpstreamRequest;
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
@RequestMapping("/api/alb/v1/upstreams")
public class AlbUpstreamController {

    private final AlbJdbcRepository repository;

    public AlbUpstreamController(AlbJdbcRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Upstream> list() {
        return repository.listUpstreams();
    }

    @PostMapping
    public Map<String, Object> create(@Valid @RequestBody UpstreamRequest request) {
        long id = repository.insertUpstream(
                request.name(),
                request.targetUrl(),
                request.websocketEnabled() == null || request.websocketEnabled(),
                request.remark());
        return Map.of("id", id);
    }

    @PutMapping("/{id}")
    public void update(@PathVariable Long id, @Valid @RequestBody UpstreamRequest request) {
        ensureExists(id);
        repository.updateUpstream(
                id,
                request.name(),
                request.targetUrl(),
                request.websocketEnabled() == null || request.websocketEnabled(),
                request.remark());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        ensureExists(id);
        if (repository.upstreamReferenced(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "上游仍被路由引用，无法删除");
        }
        repository.deleteUpstream(id);
    }

    private void ensureExists(Long id) {
        if (repository.findUpstream(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "上游不存在");
        }
    }
}
