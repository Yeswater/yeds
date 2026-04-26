package com.apig.gateway.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/mock")
public class MockUpstreamController {

    /**
     * 模拟上游受保护接口。
     */
    @GetMapping("/secure")
    public Map<String, Object> secure(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return Map.of(
                "message", "gateway secure api success",
                "clientId", jwt.getSubject(),
                "tenantId", jwt.getClaimAsString("tenant_id"),
                "scope", jwt.getClaimAsString("scope")
        );
    }

    /**
     * 模拟上游公开接口。
     */
    @GetMapping("/public")
    public Map<String, Object> pub() {
        return Map.of("message", "gateway public api success");
    }
}
