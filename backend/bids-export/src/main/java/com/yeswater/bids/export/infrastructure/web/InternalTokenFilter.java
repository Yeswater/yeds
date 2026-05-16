package com.yeswater.bids.export.infrastructure.web;

import com.yeswater.bids.export.infrastructure.config.ExportProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class InternalTokenFilter extends OncePerRequestFilter {
    public static final String TOKEN_HEADER = "X-Bids-Internal-Token";

    private final ExportProperties properties;

    public InternalTokenFilter(ExportProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/export/v1");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = request.getHeader(TOKEN_HEADER);
        if (token == null || !token.equals(properties.getInternalToken())) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"无效的内部调用令牌\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
