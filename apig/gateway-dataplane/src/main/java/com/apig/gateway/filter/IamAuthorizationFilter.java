package com.apig.gateway.filter;

import com.apig.gateway.config.GatewayProperties;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class IamAuthorizationFilter implements WebFilter, Ordered {

    private static final Logger LOGGER = LoggerFactory.getLogger(IamAuthorizationFilter.class);
    private static final String IAM_INTERNAL_HEADER = "X-Iam-Internal-Token";

    private final WebClient webClient;

    private final GatewayProperties gatewayProperties;

    public IamAuthorizationFilter(WebClient.Builder webClientBuilder, GatewayProperties gatewayProperties) {
        this.webClient = webClientBuilder.build();
        this.gatewayProperties = gatewayProperties;
    }

    /**
     * 在网关侧调用 IAM 鉴权接口，拒绝未授权请求。
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        PathPermission permission = resolvePermission(exchange);
        if (permission == null) {
            return chain.filter(exchange);
        }
        return exchange.getPrincipal()
                .cast(Authentication.class)
                .flatMap(authentication -> checkPermission(exchange, chain, authentication, permission))
                .switchIfEmpty(deny(exchange, HttpStatus.UNAUTHORIZED, "missing authentication"));
    }

    @Override
    public int getOrder() {
        return -30;
    }

    private Mono<Void> checkPermission(
            ServerWebExchange exchange,
            WebFilterChain chain,
            Authentication authentication,
            PathPermission permission
    ) {
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            return deny(exchange, HttpStatus.UNAUTHORIZED, "invalid principal");
        }
        Map<String, Object> body = new HashMap<>(4);
        body.put("userId", toLong(jwt.getSubject()));
        body.put("resource", permission.resource());
        body.put("action", permission.action());
        body.put("tenantCode", resolveTenantCode(jwt));
        String clientIp = resolveClientIp(exchange);
        return webClient.post()
                .uri(gatewayProperties.iamAuthorizeCheckUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .header(IAM_INTERNAL_HEADER, gatewayProperties.iamInternalAccessToken())
                .header("X-Forwarded-For", clientIp)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(result -> {
                    Object allowed = result.get("allowed");
                    if (Boolean.TRUE.equals(allowed)) {
                        return chain.filter(exchange);
                    }
                    Object detail = result.get("detail");
                    LOGGER.warn("IAM authorize denied, path={}, userId={}, resource={}, action={}, tenantCode={}, detail={}, clientIp={}",
                            exchange.getRequest().getPath().value(),
                            jwt.getSubject(),
                            permission.resource(),
                            permission.action(),
                            resolveTenantCode(jwt),
                            detail,
                            clientIp);
                    return deny(exchange, HttpStatus.FORBIDDEN, "permission denied");
                })
                .onErrorResume(ex -> {
                    LOGGER.warn("IAM authorize call failed: {}", ex.toString());
                    return deny(exchange, HttpStatus.SERVICE_UNAVAILABLE, "iam authorize unavailable");
                });
    }

    private PathPermission resolvePermission(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        HttpMethod method = exchange.getRequest().getMethod();
        if (path.startsWith("/api/runtime/")) {
            if (path.contains("/download")) {
                return new PathPermission("export:task", "download");
            }
            return new PathPermission("bids:model", "execute");
        }
        if (path.startsWith("/api/config/")) {
            if (HttpMethod.GET.equals(method)) {
                return new PathPermission("bids:model", "read");
            }
            return new PathPermission("bids:model", "execute");
        }
        return null;
    }

    private Long toLong(String subject) {
        try {
            return Long.parseLong(subject);
        } catch (NumberFormatException ex) {
            return -1L;
        }
    }

    private String resolveTenantCode(Jwt jwt) {
        String tenantCode = jwt.getClaimAsString("tenant_code");
        if (tenantCode != null && !tenantCode.isBlank()) {
            return tenantCode;
        }
        String tenantId = jwt.getClaimAsString("tenant_id");
        if (tenantId != null && !tenantId.isBlank()) {
            return tenantId;
        }
        return "default";
    }

    private String resolveClientIp(ServerWebExchange exchange) {
        String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        if (exchange.getRequest().getRemoteAddress() != null
                && exchange.getRequest().getRemoteAddress().getAddress() != null) {
            return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }

    private Mono<Void> deny(ServerWebExchange exchange, HttpStatus status, String message) {
        if (HttpStatus.UNAUTHORIZED.equals(status)) {
            LOGGER.warn("Gateway access denied, path={}, status={}, message={}",
                    exchange.getRequest().getPath().value(),
                    status.value(),
                    message);
        }
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = ("{\"message\":\"" + message + "\"}").getBytes();
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                .bufferFactory()
                .wrap(body)));
    }

    private record PathPermission(String resource, String action) {
    }
}
