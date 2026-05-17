package com.yeswater.iam.interfaces.rest;

import com.yeswater.iam.application.AuthApplicationService;
import com.yeswater.iam.config.JwtKeyProvider;
import com.yeswater.iam.interfaces.dto.AuthTokenResponse;
import com.yeswater.iam.interfaces.dto.LoginRequest;
import com.yeswater.iam.interfaces.dto.LogoutRequest;
import com.yeswater.iam.interfaces.dto.MfaSendOtpRequest;
import com.yeswater.iam.interfaces.dto.OidcAuthorizeRequest;
import com.yeswater.iam.interfaces.dto.OidcTokenRequest;
import com.yeswater.iam.interfaces.dto.RefreshTokenRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/iam/auth")
public class AuthController {

    private final AuthApplicationService authApplicationService;

    private final JwtKeyProvider jwtKeyProvider;

    public AuthController(AuthApplicationService authApplicationService, JwtKeyProvider jwtKeyProvider) {
        this.authApplicationService = authApplicationService;
        this.jwtKeyProvider = jwtKeyProvider;
    }

    /**
     * 用户名密码登录。
     */
    @PostMapping("/login")
    public ResponseEntity<AuthTokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpServletRequest
    ) {
        AuthTokenResponse response = authApplicationService.login(
                request.username(),
                request.password(),
                request.otpCode(),
                resolveClientIp(httpServletRequest)
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 刷新访问令牌。
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthTokenResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpServletRequest
    ) {
        AuthTokenResponse response = authApplicationService.refresh(request.refreshToken(), resolveClientIp(httpServletRequest));
        return ResponseEntity.ok(response);
    }

    /**
     * 吊销刷新令牌。
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @Valid @RequestBody LogoutRequest request,
            HttpServletRequest httpServletRequest
    ) {
        authApplicationService.logout(request.refreshToken(), resolveClientIp(httpServletRequest));
        return ResponseEntity.ok(Map.of("message", "logout success"));
    }

    /**
     * 返回网关可消费的 JWK 公钥。
     */
    @GetMapping("/jwks")
    public ResponseEntity<Map<String, Object>> jwks() {
        String modulus = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(jwtKeyProvider.getPublicKey().getModulus().toByteArray());
        String exponent = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(jwtKeyProvider.getPublicKey().getPublicExponent().toByteArray());
        Map<String, Object> jwk = Map.of(
                "kty", "RSA",
                "use", "sig",
                "alg", "RS256",
                "kid", jwtKeyProvider.getKeyId(),
                "n", modulus,
                "e", exponent
        );
        return ResponseEntity.ok(Map.of("keys", List.of(jwk)));
    }

    /**
     * 发送 MFA 验证码（演示接口，返回验证码）。
     */
    @PostMapping("/mfa/send-otp")
    public ResponseEntity<Map<String, Object>> sendOtp(
            @Valid @RequestBody MfaSendOtpRequest request,
            HttpServletRequest httpServletRequest
    ) {
        String otpCode = authApplicationService.sendMfaOtp(request.username(), resolveClientIp(httpServletRequest));
        return ResponseEntity.ok(Map.of("otpCode", otpCode, "message", "mfa otp sent"));
    }

    /**
     * 模拟 OIDC 授权端签发 code。
     */
    @PostMapping("/oidc/authorize")
    public ResponseEntity<Map<String, Object>> oidcAuthorize(
            @Valid @RequestBody OidcAuthorizeRequest request,
            HttpServletRequest httpServletRequest
    ) {
        String authorizationCode = authApplicationService.authorizeOidc(
                request.issuer(),
                request.externalSubject(),
                request.externalTenant(),
                request.username(),
                resolveClientIp(httpServletRequest)
        );
        return ResponseEntity.ok(Map.of("authorizationCode", authorizationCode));
    }

    /**
     * OIDC code 换取平台令牌。
     */
    @PostMapping("/oidc/token")
    public ResponseEntity<AuthTokenResponse> oidcToken(
            @Valid @RequestBody OidcTokenRequest request,
            HttpServletRequest httpServletRequest
    ) {
        AuthTokenResponse response = authApplicationService.exchangeOidcCode(
                request.authorizationCode(),
                resolveClientIp(httpServletRequest)
        );
        return ResponseEntity.ok(response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
