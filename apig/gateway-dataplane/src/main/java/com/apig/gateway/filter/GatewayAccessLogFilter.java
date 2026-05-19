package com.apig.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 记录网关请求路径、状态码与耗时。
 */
@Component
public class GatewayAccessLogFilter implements GlobalFilter, Ordered {

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayAccessLogFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startNanos = System.nanoTime();
        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethod() == null ? "UNKNOWN" : request.getMethod().name();
        String path = request.getURI().getRawPath();
        return chain.filter(exchange)
                .doOnSuccess(ignored -> logAccess(method, path, exchange.getResponse().getStatusCode() == null
                        ? 0
                        : exchange.getResponse().getStatusCode().value(), startNanos))
                .doOnError(error -> {
                    logAccess(method, path, exchange.getResponse().getStatusCode() == null
                            ? 0
                            : exchange.getResponse().getStatusCode().value(), startNanos);
                    LOGGER.warn("网关转发异常, method={}, path={}, message={}", method, path, error.toString());
                });
    }

    private void logAccess(String method, String path, int status, long startNanos) {
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
        if (status >= 500) {
            LOGGER.error("网关访问, method={}, path={}, status={}, elapsedMs={}", method, path, status, elapsedMs);
        } else if (status >= 400) {
            LOGGER.warn("网关访问, method={}, path={}, status={}, elapsedMs={}", method, path, status, elapsedMs);
        } else {
            LOGGER.info("网关访问, method={}, path={}, status={}, elapsedMs={}", method, path, status, elapsedMs);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
