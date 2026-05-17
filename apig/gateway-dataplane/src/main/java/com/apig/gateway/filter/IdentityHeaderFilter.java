package com.apig.gateway.filter;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
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
                .flatMap(authentication -> chain.filter(exchange))
                .switchIfEmpty(chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        return -20;
    }
}
