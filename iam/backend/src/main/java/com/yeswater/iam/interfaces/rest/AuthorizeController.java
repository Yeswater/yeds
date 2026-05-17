package com.yeswater.iam.interfaces.rest;

import com.yeswater.iam.application.AuthorizeApplicationService;
import com.yeswater.iam.interfaces.dto.AuthorizeBatchCheckRequest;
import com.yeswater.iam.interfaces.dto.AuthorizeCheckRequest;
import com.yeswater.iam.interfaces.dto.AuthorizeCheckResponse;
import com.yeswater.iam.interfaces.dto.UserPermissionsResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/iam/authorize")
public class AuthorizeController {

    private final AuthorizeApplicationService authorizeApplicationService;

    public AuthorizeController(AuthorizeApplicationService authorizeApplicationService) {
        this.authorizeApplicationService = authorizeApplicationService;
    }

    /**
     * 单个权限检查。
     */
    @PostMapping("/check")
    public ResponseEntity<AuthorizeCheckResponse> check(
            @Valid @RequestBody AuthorizeCheckRequest request,
            HttpServletRequest httpServletRequest
    ) {
        AuthorizeCheckRequest enrichedRequest = new AuthorizeCheckRequest(
                request.userId(),
                request.resource(),
                request.action(),
                resolveEnvTag(request.envTag(), httpServletRequest),
                resolveTenantCode(request.tenantCode(), httpServletRequest),
                request.attributes()
        );
        AuthorizeCheckResponse response = authorizeApplicationService.check(enrichedRequest, resolveClientIp(httpServletRequest));
        return ResponseEntity.ok(response);
    }

    /**
     * 批量权限检查。
     */
    @PostMapping("/batch-check")
    public ResponseEntity<List<AuthorizeCheckResponse>> batchCheck(
            @Valid @RequestBody AuthorizeBatchCheckRequest request,
            HttpServletRequest httpServletRequest
    ) {
        List<AuthorizeCheckResponse> responses = request.items().stream()
                .map(item -> new AuthorizeCheckRequest(
                        item.userId(),
                        item.resource(),
                        item.action(),
                        resolveEnvTag(item.envTag(), httpServletRequest),
                        resolveTenantCode(item.tenantCode(), httpServletRequest),
                        item.attributes()
                ))
                .map(item -> authorizeApplicationService.check(item, resolveClientIp(httpServletRequest)))
                .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * 查询用户权限编码清单。
     */
    @GetMapping("/user-permissions")
    public ResponseEntity<UserPermissionsResponse> userPermissions(@RequestParam("userId") Long userId) {
        return ResponseEntity.ok(authorizeApplicationService.listUserPermissions(userId));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveEnvTag(String envTag, HttpServletRequest request) {
        if (envTag != null && !envTag.isBlank()) {
            return envTag;
        }
        String fromHeader = request.getHeader("X-Env-Tag");
        return fromHeader == null || fromHeader.isBlank() ? "dev" : fromHeader;
    }

    private String resolveTenantCode(String tenantCode, HttpServletRequest request) {
        if (tenantCode != null && !tenantCode.isBlank()) {
            return tenantCode;
        }
        String fromHeader = request.getHeader("X-Bids-Tenant");
        return fromHeader == null || fromHeader.isBlank() ? "default" : fromHeader;
    }
}
