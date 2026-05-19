package com.yeswater.alb.dataplane.proxy;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AlbProxyWebFilter implements WebFilter {

    private final AlbProxyHandler proxyHandler;

    public AlbProxyWebFilter(AlbProxyHandler proxyHandler) {
        this.proxyHandler = proxyHandler;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith("/actuator")) {
            return chain.filter(exchange);
        }
        return proxyHandler.handle(exchange);
    }
}
