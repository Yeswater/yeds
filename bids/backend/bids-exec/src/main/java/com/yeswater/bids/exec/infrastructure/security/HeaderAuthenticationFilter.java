package com.yeswater.bids.exec.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeaderAuthenticationFilter.class);

    private final String userHeader;
    private final String userIdHeader;
    private final String rolesHeader;
    private final String trustedTokenHeader;
    private final String trustedTokenValue;
    private final boolean enabled;

    public HeaderAuthenticationFilter(
            @Value("${bids.security.user-header:X-Bids-User}") String userHeader,
            @Value("${bids.security.user-id-header:X-Bids-User-Id}") String userIdHeader,
            @Value("${bids.security.roles-header:X-Bids-Roles}") String rolesHeader,
            @Value("${bids.security.trusted-header.token-header:X-Bids-Trusted-Token}") String trustedTokenHeader,
            @Value("${bids.security.trusted-header.token:change-me}") String trustedTokenValue,
            @Value("${bids.security.trusted-header.enabled:false}") boolean enabled
    ) {
        this.userHeader = userHeader;
        this.userIdHeader = userIdHeader;
        this.rolesHeader = rolesHeader;
        this.trustedTokenHeader = trustedTokenHeader;
        this.trustedTokenValue = trustedTokenValue;
        this.enabled = enabled;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }
        String requestTrustedToken = request.getHeader(trustedTokenHeader);
        if (trustedTokenValue == null || trustedTokenValue.isBlank() || !trustedTokenValue.equals(requestTrustedToken)) {
            LOGGER.warn("受信头校验失败, path={}, hasTrustedToken={}",
                    request.getRequestURI(),
                    requestTrustedToken != null && !requestTrustedToken.isBlank());
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        String username = defaultIfBlank(request.getHeader(userHeader), request.getHeader(userIdHeader));
        if (username != null && !username.isBlank()) {
            List<SimpleGrantedAuthority> authorities = Arrays.stream(defaultIfBlank(request.getHeader(rolesHeader), "USER").split(","))
                    .map(String::trim)
                    .filter(role -> !role.isEmpty())
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                    .map(SimpleGrantedAuthority::new)
                    .toList();
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(username, null, authorities)
            );
        }
        filterChain.doFilter(request, response);
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
