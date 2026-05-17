package com.apig.gateway.filter;

import com.apig.gateway.config.GatewayProperties;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SimpleRateLimitFilter implements WebFilter, Ordered {

    private final GatewayProperties properties;
    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public SimpleRateLimitFilter(GatewayProperties properties) {
        this.properties = properties;
    }

    /**
     * 对已认证调用方做每分钟窗口限流。
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!exchange.getRequest().getPath().value().startsWith("/api/")) {
            return chain.filter(exchange);
        }
        return exchange.getPrincipal()
                .cast(Authentication.class)
                .map(authentication -> ((Jwt) authentication.getPrincipal()).getSubject())
                .defaultIfEmpty("anonymous")
                .flatMap(subject -> {
                    if (isRejected(subject)) {
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        return exchange.getResponse().setComplete();
                    }
                    return chain.filter(exchange);
                });
    }

    @Override
    public int getOrder() {
        return -10;
    }

    private boolean isRejected(String subject) {
        long currentMinute = Instant.now().getEpochSecond() / 60;
        WindowCounter counter = counters.compute(subject, (k, oldValue) -> {
            if (oldValue == null || oldValue.minute != currentMinute) {
                return new WindowCounter(currentMinute, new AtomicInteger(1));
            }
            oldValue.counter.incrementAndGet();
            return oldValue;
        });
        return counter.counter.get() > properties.rateLimitPerMinute();
    }

    private static final class WindowCounter {
        private final long minute;
        private final AtomicInteger counter;

        private WindowCounter(long minute, AtomicInteger counter) {
            this.minute = minute;
            this.counter = counter;
        }
    }
}
