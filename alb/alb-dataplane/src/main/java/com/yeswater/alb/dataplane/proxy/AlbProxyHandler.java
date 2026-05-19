package com.yeswater.alb.dataplane.proxy;

import com.yeswater.alb.dataplane.routing.HeaderPolicyApplier;
import com.yeswater.alb.dataplane.routing.RouteMatcher;
import com.yeswater.alb.dataplane.snapshot.RoutingSnapshot;
import com.yeswater.alb.dataplane.snapshot.SnapshotStore;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@Component
public class AlbProxyHandler {

    private final SnapshotStore snapshotStore;
    private final RouteMatcher routeMatcher;
    private final HeaderPolicyApplier headerPolicyApplier;
    private final WebClient webClient;

    public AlbProxyHandler(SnapshotStore snapshotStore, RouteMatcher routeMatcher, HeaderPolicyApplier headerPolicyApplier) {
        this.snapshotStore = snapshotStore;
        this.routeMatcher = routeMatcher;
        this.headerPolicyApplier = headerPolicyApplier;
        this.webClient = WebClient.builder().build();
    }

    /**
     * 处理入站请求：匹配路由、应用 Header 策略并反代。
     */
    public Mono<Void> handle(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        String host = Optional.ofNullable(request.getHeaders().getFirst(HttpHeaders.HOST)).orElse("");
        String path = request.getURI().getPath();

        Optional<RoutingSnapshot.SnapshotRoute> matched = routeMatcher.match(
                host,
                path,
                snapshotStore.routes());
        if (matched.isEmpty()) {
            exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory()
                    .wrap(("未匹配路由: host=" + host + ", path=" + path).getBytes())));
        }

        RoutingSnapshot.SnapshotRoute route = matched.get();
        if (route.redirectUrl() != null && !route.redirectUrl().isBlank()) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
            response.getHeaders().set(HttpHeaders.LOCATION, route.redirectUrl() + (path.equals("/") ? "" : path));
            return response.setComplete();
        }

        URI targetUri = buildTargetUri(route, request);
        HttpHeaders forwardHeaders = new HttpHeaders();
        forwardHeaders.putAll(request.getHeaders());
        headerPolicyApplier.apply(forwardHeaders, route.headers(), "request");
        forwardHeaders.set(HttpHeaders.HOST, targetUri.getHost() + (targetUri.getPort() > 0 ? ":" + targetUri.getPort() : ""));

        return webClient.method(HttpMethod.valueOf(request.getMethod().name()))
                .uri(targetUri)
                .headers(headers -> headers.putAll(forwardHeaders))
                .body(BodyInserters.fromDataBuffers(request.getBody()))
                .exchangeToMono(clientResponse -> {
                    ServerHttpResponse response = exchange.getResponse();
                    response.setStatusCode(clientResponse.statusCode());
                    HttpHeaders responseHeaders = new HttpHeaders();
                    responseHeaders.putAll(clientResponse.headers().asHttpHeaders());
                    headerPolicyApplier.apply(responseHeaders, route.headers(), "response");
                    response.getHeaders().putAll(responseHeaders);
                    return response.writeWith(clientResponse.bodyToFlux(DataBuffer.class));
                });
    }

    private URI buildTargetUri(RoutingSnapshot.SnapshotRoute route, ServerHttpRequest request) {
        URI upstreamBase = URI.create(route.targetUrl());
        String requestPath = request.getURI().getRawPath();
        String query = request.getURI().getRawQuery();
        String forwardedPath = requestPath;
        if (route.stripPrefix() && route.pathPattern() != null && !"/**".equals(route.pathPattern())) {
            String prefix = route.pathPattern().endsWith("/**")
                    ? route.pathPattern().substring(0, route.pathPattern().length() - 3)
                    : route.pathPattern();
            if (forwardedPath.startsWith(prefix)) {
                forwardedPath = forwardedPath.substring(prefix.length());
                if (forwardedPath.isEmpty()) {
                    forwardedPath = "/";
                }
            }
        }
        StringBuilder target = new StringBuilder();
        target.append(upstreamBase.getScheme()).append("://").append(upstreamBase.getAuthority());
        if (upstreamBase.getPath() != null && !upstreamBase.getPath().isBlank() && !"/".equals(upstreamBase.getPath())) {
            target.append(upstreamBase.getPath());
        }
        target.append(forwardedPath.startsWith("/") ? forwardedPath : "/" + forwardedPath);
        if (query != null && !query.isBlank()) {
            target.append('?').append(query);
        }
        return URI.create(target.toString());
    }
}
