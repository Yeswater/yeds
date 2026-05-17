package com.apig.gateway.filter;

import com.apig.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class IdentityHeaderFilter implements GlobalFilter, Ordered {

    private static final String USER_HEADER = "X-Bids-User";
    private static final String USER_ID_HEADER = "X-Bids-User-Id";
    private static final String ROLES_HEADER = "X-Bids-Roles";
    private static final String TENANT_HEADER = "X-Bids-Tenant";
    private static final String TRUSTED_TOKEN_HEADER = "X-Bids-Trusted-Token";

    private final GatewayProperties gatewayProperties;

    public IdentityHeaderFilter(GatewayProperties gatewayProperties) {
        this.gatewayProperties = gatewayProperties;
    }

    /**
     * 将认证主体信息注入到下游请求头。
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        return exchange.getPrincipal()
                .cast(Authentication.class)
                .flatMap(authentication -> chain.filter(mutateExchange(exchange, authentication)))
                .switchIfEmpty(chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        return -20;
    }

    private ServerWebExchange mutateExchange(ServerWebExchange exchange, Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            return exchange;
        }
        String username = defaultString(jwt.getClaimAsString("username"), jwt.getSubject());
        String roleHeader = normalizeRoles(jwt.getClaimAsStringList("roles"));
        String tenantCode = resolveTenantCode(jwt);
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(exchange.getRequest().getHeaders());
        headers.set(USER_HEADER, username);
        headers.set(USER_ID_HEADER, jwt.getSubject());
        headers.set(ROLES_HEADER, roleHeader);
        headers.set(TENANT_HEADER, tenantCode);
        headers.set(TRUSTED_TOKEN_HEADER, gatewayProperties.trustedHeaderToken());

        ServerHttpRequest request = new ServerHttpRequestDecorator(exchange.getRequest()) {
            @Override
            public HttpHeaders getHeaders() {
                return headers;
            }
        };
        return exchange.mutate()
                .request(request)
                .build();
    }

    private String defaultString(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String resolveTenantCode(Jwt jwt) {
        String tenantCode = jwt.getClaimAsString("tenant_code");
        if (tenantCode != null && !tenantCode.isBlank()) {
            return tenantCode;
        }
        return defaultString(jwt.getClaimAsString("tenant_id"), "default");
    }

    private String normalizeRoles(java.util.List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return "USER";
        }
        return roles.stream()
                .filter(role -> role != null && !role.isBlank())
                .map(String::trim)
                .map(role -> role.startsWith("ROLE_") ? role.substring("ROLE_".length()) : role)
                .distinct()
                .reduce((left, right) -> left + "," + right)
                .orElse("USER");
    }
}
