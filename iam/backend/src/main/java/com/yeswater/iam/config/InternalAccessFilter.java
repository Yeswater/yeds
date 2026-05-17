package com.yeswater.iam.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class InternalAccessFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(InternalAccessFilter.class);

    private final boolean enabled;
    private final String headerName;
    private final String token;

    public InternalAccessFilter(
            @Value("${iam.internal-access.enabled:true}") boolean enabled,
            @Value("${iam.internal-access.header-name:X-Iam-Internal-Token}") String headerName,
            @Value("${iam.internal-access.token:yeds-iam-internal-v1}") String token
    ) {
        this.enabled = enabled;
        this.headerName = headerName;
        this.token = token;
    }

    /**
     * 仅对授权与管理接口启用内部令牌校验。
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        if (!enabled) {
            return true;
        }
        String path = request.getRequestURI();
        return !(path.startsWith("/api/iam/authorize/") || path.startsWith("/api/iam/manage/"));
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    )
            throws ServletException, IOException {
        if (token == null || token.isBlank()) {
            logDeny("iam internal access token is not configured");
            writeForbidden(response, "iam internal access token is not configured");
            return;
        }
        String incomingToken = request.getHeader(headerName);
        if (!token.equals(incomingToken)) {
            logDeny("iam internal access denied");
            writeForbidden(response, "iam internal access denied");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void writeForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }

    private void logDeny(String message) {
        LOGGER.warn("IAM内部访问拒绝, traceId={}, message={}", MDC.get("traceId"), message);
    }
}
