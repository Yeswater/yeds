package com.apig.auth.service;

import com.apig.auth.config.AuthProperties;
import com.apig.auth.model.ClientInfo;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final AuthProperties properties;
    private final SecretKey secretKey;

    public JwtService(AuthProperties properties) {
        this.properties = properties;
        this.secretKey = Keys.hmacShaKeyFor(properties.jwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成机机访问场景的 JWT 令牌。
     */
    public String generateClientToken(ClientInfo client) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(properties.tokenExpireSeconds());
        return Jwts.builder()
                .issuer("apig-auth-service")
                .subject(client.clientId())
                .audience().add("apig-gateway").and()
                .claim("tenant_id", client.tenantId())
                .claim("scope", String.join(" ", client.scopes()))
                .claim("subject_type", "workload")
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .signWith(secretKey)
                .compact();
    }
}
