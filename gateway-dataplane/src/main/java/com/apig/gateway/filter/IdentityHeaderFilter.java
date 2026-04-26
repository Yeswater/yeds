package com.apig.gateway.filter;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class IdentityHeaderFilter implements GlobalFilter, Ordered {

    /**
     * 将认证主体信息注入到下游请求头。
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        return exchange.getPrincipal()
                .cast(org.springframework.security.core.Authentication.class)
                .flatMap(authentication -> {
                    Jwt jwt = (Jwt) authentication.getPrincipal();
                    ServerWebExchange mutated = exchange.mutate()
                            .request(request -> request.headers(httpHeaders -> {
                                httpHeaders.set("x-client-id", jwt.getSubject());
                                httpHeaders.set("x-tenant-id", jwt.getClaimAsString("tenant_id"));
                                httpHeaders.set("x-subject-type", jwt.getClaimAsString("subject_type"));
                            }))
                            .build();
                    return chain.filter(mutated);
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        return -20;
    }
}
