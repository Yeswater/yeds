package com.yeswater.iam.application;

import com.yeswater.iam.config.JwtProperties;
import com.yeswater.iam.config.SecurityProperties;
import com.yeswater.iam.domain.exception.BusinessException;
import com.yeswater.iam.domain.model.IamUserInfo;
import com.yeswater.iam.domain.model.OidcAuthCodeInfo;
import com.yeswater.iam.domain.model.TokenSessionInfo;
import com.yeswater.iam.infrastructure.repository.IamJdbcRepository;
import com.yeswater.iam.interfaces.dto.AuthTokenResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AuthApplicationService {

    private final IamJdbcRepository iamJdbcRepository;

    private final AuthTokenService authTokenService;

    private final JwtProperties jwtProperties;

    private final SecurityProperties securityProperties;

    public AuthApplicationService(
            IamJdbcRepository iamJdbcRepository,
            AuthTokenService authTokenService,
            JwtProperties jwtProperties,
            SecurityProperties securityProperties
    ) {
        this.iamJdbcRepository = iamJdbcRepository;
        this.authTokenService = authTokenService;
        this.jwtProperties = jwtProperties;
        this.securityProperties = securityProperties;
    }

    /**
     * 处理登录并签发令牌。
     */
    public AuthTokenResponse login(String username, String password, String otpCode, String clientIp) {
        IamUserInfo userInfo = iamJdbcRepository.findUserByUsername(username)
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));
        if (!password.equals(userInfo.password())) {
            iamJdbcRepository.saveAuditLog("AUTH_LOGIN", username, null, null, null, "DENY", "密码错误", clientIp);
            throw new BusinessException("用户名或密码错误");
        }
        if (userInfo.status() == null || userInfo.status() != 1) {
            iamJdbcRepository.saveAuditLog("AUTH_LOGIN", username, userInfo.id(), null, null, "DENY", "用户已禁用", clientIp);
            throw new BusinessException("账号不可用");
        }
        if (userInfo.mfaEnabled() != null && userInfo.mfaEnabled() == 1) {
            if (otpCode == null || otpCode.isBlank()) {
                iamJdbcRepository.saveAuditLog("AUTH_LOGIN", username, userInfo.id(), null, null, "DENY", "MFA验证码缺失", clientIp);
                throw new BusinessException("请先输入MFA验证码");
            }
            if (!iamJdbcRepository.consumeMfaOtp(userInfo.id(), otpCode)) {
                iamJdbcRepository.saveAuditLog("AUTH_LOGIN", username, userInfo.id(), null, null, "DENY", "MFA验证码错误", clientIp);
                throw new BusinessException("MFA验证码无效或已过期");
            }
        }

        List<String> roleCodes = iamJdbcRepository.listRoleCodesByUserId(userInfo.id());
        String accessToken = authTokenService.generateAccessToken(
                userInfo.id(),
                userInfo.username(),
                userInfo.tenantCode(),
                roleCodes
        );
        String refreshToken = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime refreshExpiredAt = LocalDateTime.now().plusSeconds(jwtProperties.getRefreshTokenTtlSeconds());
        iamJdbcRepository.saveTokenSession(userInfo.id(), refreshToken, refreshExpiredAt);
        detectAbnormalLogin(userInfo, clientIp);
        iamJdbcRepository.updateUserLastLogin(userInfo.id(), clientIp);
        iamJdbcRepository.saveAuditLog("AUTH_LOGIN", username, userInfo.id(), null, null, "ALLOW", "登录成功", clientIp);
        return new AuthTokenResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtProperties.getAccessTokenTtlSeconds()
        );
    }

    /**
     * 发送一次性 MFA 验证码（演示模式返回明文验证码）。
     */
    public String sendMfaOtp(String username, String clientIp) {
        IamUserInfo userInfo = iamJdbcRepository.findUserByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        if (userInfo.mfaEnabled() == null || userInfo.mfaEnabled() != 1) {
            throw new BusinessException("该账号未开启MFA");
        }
        String otpCode = String.valueOf((int) ((Math.random() * 900000) + 100000));
        LocalDateTime expiredAt = LocalDateTime.now().plusSeconds(securityProperties.getMfaOtpTtlSeconds());
        iamJdbcRepository.saveMfaOtp(userInfo.id(), otpCode, expiredAt);
        iamJdbcRepository.saveAuditLog("AUTH_MFA_SEND_OTP", username, userInfo.id(), null, null, "ALLOW", "MFA验证码已发送", clientIp);
        return otpCode;
    }

    /**
     * 生成 OIDC 授权码。
     */
    public String authorizeOidc(String issuer, String externalSubject, String externalTenant, String username, String clientIp) {
        IamUserInfo userInfo = iamJdbcRepository.findUserByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        iamJdbcRepository.findActiveTenantFederation(userInfo.tenantCode(), issuer, externalTenant)
                .orElseThrow(() -> new BusinessException("租户联邦映射不存在或未启用"));
        Long userId = iamJdbcRepository.findUserIdByOidcIdentity(issuer, externalSubject)
                .orElseGet(() -> {
                    iamJdbcRepository.saveOidcIdentity(issuer, externalSubject, userInfo.id());
                    return userInfo.id();
                });
        String authCode = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiredAt = LocalDateTime.now().plusSeconds(securityProperties.getOidcAuthCodeTtlSeconds());
        iamJdbcRepository.saveOidcAuthCode(authCode, issuer, externalSubject, userId, expiredAt);
        iamJdbcRepository.saveAuditLog(
                "AUTH_OIDC_AUTHORIZE",
                username,
                userId,
                null,
                null,
                "ALLOW",
                "OIDC授权码签发成功",
                clientIp
        );
        return authCode;
    }

    /**
     * 使用 OIDC 授权码换取平台令牌。
     */
    public AuthTokenResponse exchangeOidcCode(String authorizationCode, String clientIp) {
        OidcAuthCodeInfo authCodeInfo = iamJdbcRepository.findOidcAuthCode(authorizationCode)
                .orElseThrow(() -> new BusinessException("OIDC授权码无效"));
        if (authCodeInfo.used() != null && authCodeInfo.used() == 1) {
            throw new BusinessException("OIDC授权码已使用");
        }
        if (authCodeInfo.expiredAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("OIDC授权码已过期");
        }
        IamUserInfo userInfo = iamJdbcRepository.findUserById(authCodeInfo.userId())
                .orElseThrow(() -> new BusinessException("用户不存在"));
        iamJdbcRepository.markOidcAuthCodeUsed(authCodeInfo.id());
        List<String> roleCodes = iamJdbcRepository.listRoleCodesByUserId(userInfo.id());
        String accessToken = authTokenService.generateAccessToken(
                userInfo.id(),
                userInfo.username(),
                userInfo.tenantCode(),
                roleCodes
        );
        String refreshToken = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime refreshExpiredAt = LocalDateTime.now().plusSeconds(jwtProperties.getRefreshTokenTtlSeconds());
        iamJdbcRepository.saveTokenSession(userInfo.id(), refreshToken, refreshExpiredAt);
        iamJdbcRepository.saveAuditLog(
                "AUTH_OIDC_TOKEN",
                userInfo.username(),
                userInfo.id(),
                null,
                null,
                "ALLOW",
                "OIDC换取平台令牌成功",
                clientIp
        );
        return new AuthTokenResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtProperties.getAccessTokenTtlSeconds()
        );
    }

    /**
     * 使用刷新令牌续签访问令牌。
     */
    public AuthTokenResponse refresh(String refreshToken, String clientIp) {
        TokenSessionInfo tokenSessionInfo = iamJdbcRepository.findTokenSessionByRefreshToken(refreshToken)
                .orElseThrow(() -> new BusinessException("refreshToken无效"));
        if (tokenSessionInfo.revoked() != null && tokenSessionInfo.revoked() == 1) {
            throw new BusinessException("refreshToken已吊销");
        }
        if (tokenSessionInfo.expiredAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("refreshToken已过期");
        }

        IamUserInfo userInfo = iamJdbcRepository.findUserById(tokenSessionInfo.userId())
                .orElseThrow(() -> new BusinessException("用户不存在"));
        List<String> roleCodes = iamJdbcRepository.listRoleCodesByUserId(userInfo.id());
        String accessToken = authTokenService.generateAccessToken(
                userInfo.id(),
                userInfo.username(),
                userInfo.tenantCode(),
                roleCodes
        );
        iamJdbcRepository.saveAuditLog("AUTH_REFRESH", userInfo.username(), userInfo.id(), null, null, "ALLOW", "令牌刷新成功", clientIp);
        return new AuthTokenResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtProperties.getAccessTokenTtlSeconds()
        );
    }

    /**
     * 执行登录会话吊销。
     */
    public void logout(String refreshToken, String clientIp) {
        TokenSessionInfo tokenSessionInfo = iamJdbcRepository.findTokenSessionByRefreshToken(refreshToken)
                .orElseThrow(() -> new BusinessException("refreshToken无效"));
        iamJdbcRepository.revokeTokenSession(refreshToken);
        iamJdbcRepository.findUserById(tokenSessionInfo.userId()).ifPresent(user ->
                iamJdbcRepository.saveAuditLog("AUTH_LOGOUT", user.username(), user.id(), null, null, "ALLOW", "退出登录成功", clientIp)
        );
    }

    private void detectAbnormalLogin(IamUserInfo userInfo, String clientIp) {
        if (userInfo.lastLoginIp() == null || userInfo.lastLoginIp().isBlank()) {
            return;
        }
        if (clientIp == null || clientIp.isBlank()) {
            return;
        }
        if (!userInfo.lastLoginIp().equals(clientIp)) {
            iamJdbcRepository.saveRiskEvent(
                    "ABNORMAL_LOGIN",
                    userInfo.id(),
                    userInfo.tenantCode(),
                    "HIGH",
                    "登录IP发生变化: " + userInfo.lastLoginIp() + " -> " + clientIp
            );
        }
    }
}
