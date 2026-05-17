package com.apig.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(GatewayProperties.class)
public class SecurityConfig {

    /**
     * 配置网关资源服务鉴权规则。
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/actuator/**", "/public/**").permitAll()
                        .pathMatchers("/iam/**").permitAll()
                        .pathMatchers("/api/iam/auth/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/mock/public", "/api/mock/public").permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
                }))
                .build();
    }

    /**
     * 使用 IAM JWK 进行 JWT 验签。
     */
    @Bean
    public ReactiveJwtDecoder jwtDecoder(GatewayProperties properties) {
        return NimbusReactiveJwtDecoder.withJwkSetUri(properties.iamJwkSetUri()).build();
    }
}
