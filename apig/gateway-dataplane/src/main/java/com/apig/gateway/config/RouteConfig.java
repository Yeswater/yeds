package com.apig.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    /**
     * 定义 MVP 路由规则。
     */
    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder, GatewayProperties properties) {
        return builder.routes()
                .route("iam-backend", r -> r.path("/api/iam/**")
                        .uri(properties.iamBaseUrl()))
                .route("iam-login-page", r -> r.path("/iam/**")
                        .uri(properties.iamBaseUrl()))
                .route("bids-config", r -> r.path("/api/config/**")
                        .uri(properties.bidsConfigBaseUrl()))
                .route("bids-exec", r -> r.path("/api/runtime/**")
                        .uri(properties.bidsExecBaseUrl()))
                .route("mock-upstream", r -> r.path("/api/mock/**")
                        .filters(f -> f.rewritePath("/api/mock/(?<segment>.*)", "/mock/${segment}"))
                        .uri("http://127.0.0.1:8080"))
                .build();
    }
}
