package com.yeswater.iam.application;

import com.yeswater.iam.config.JwtKeyProvider;
import com.yeswater.iam.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
public class AuthTokenService {

    private final JwtProperties jwtProperties;

    private final JwtKeyProvider jwtKeyProvider;

    public AuthTokenService(JwtProperties jwtProperties, JwtKeyProvider jwtKeyProvider) {
        this.jwtProperties = jwtProperties;
        this.jwtKeyProvider = jwtKeyProvider;
    }

    /**
     * 生成访问令牌。
     */
    public String generateAccessToken(Long userId, String username, String tenantCode, List<String> roles) {
        Instant now = Instant.now();
        Instant expiredAt = now.plusSeconds(jwtProperties.getAccessTokenTtlSeconds());
        return Jwts.builder()
                .header().keyId(jwtKeyProvider.getKeyId()).and()
                .subject(String.valueOf(userId))
                .issuer(jwtProperties.getIssuer())
                .audience().add(jwtProperties.getAudience()).and()
                .claim("username", username)
                .claim("tenant_code", tenantCode)
                .claim("tenant_id", tenantCode)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiredAt))
                .signWith(jwtKeyProvider.getPrivateKey(), SignatureAlgorithm.RS256)
                .compact();
    }
}
