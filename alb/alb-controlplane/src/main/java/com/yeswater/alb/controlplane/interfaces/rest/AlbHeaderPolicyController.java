package com.yeswater.alb.controlplane.interfaces.rest;

import com.yeswater.alb.controlplane.domain.HeaderPolicy;
import com.yeswater.alb.controlplane.interfaces.dto.HeaderPolicyRequest;
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
@RequestMapping("/api/alb/v1")
public class AlbHeaderPolicyController {

    private final AlbJdbcRepository repository;

    public AlbHeaderPolicyController(AlbJdbcRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/routes/{routeId}/headers")
    public List<HeaderPolicy> list(@PathVariable Long routeId) {
        ensureRoute(routeId);
        return repository.listHeaderPolicies(routeId);
    }

    @PostMapping("/routes/{routeId}/headers")
    public Map<String, Object> create(@PathVariable Long routeId, @Valid @RequestBody HeaderPolicyRequest request) {
        ensureRoute(routeId);
        validateOp(request.op());
        long id = repository.insertHeaderPolicy(
                routeId,
                request.direction(),
                request.op(),
                request.headerKey(),
                request.headerValue(),
                request.sortOrder() == null ? 0 : request.sortOrder(),
                request.enabled() == null || request.enabled());
        return Map.of("id", id);
    }

    @PutMapping("/headers/{id}")
    public void update(@PathVariable Long id, @Valid @RequestBody HeaderPolicyRequest request) {
        validateOp(request.op());
        if (repository.updateHeaderPolicy(
                id,
                request.direction(),
                request.op(),
                request.headerKey(),
                request.headerValue(),
                request.sortOrder() == null ? 0 : request.sortOrder(),
                request.enabled() == null || request.enabled()) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Header 策略不存在");
        }
    }

    @DeleteMapping("/headers/{id}")
    public void delete(@PathVariable Long id) {
        if (repository.deleteHeaderPolicy(id) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Header 策略不存在");
        }
    }

    private void ensureRoute(Long routeId) {
        if (repository.findRoute(routeId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "路由不存在");
        }
    }

    private void validateOp(String op) {
        if (!List.of("add", "set", "remove").contains(op)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "op 必须为 add/set/remove");
        }
    }
}
