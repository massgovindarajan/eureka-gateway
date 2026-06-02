package com.eureka_Gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.List;

@Slf4j
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh-token",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/notifications/stream",
            "/api/doctors",
            "/api/slots",
            "/api/payments/create-order",
            "/api/payments/verify",
            "/api/payments/webhook",
            "/uploads",
            "/actuator/health",
            "/actuator/info",
            "/fallback"
    );

    @Value("${jwt.secret}")
    private String jwtSecret;

    private Key signingKey;

    @PostConstruct
    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        log.info("JwtAuthFilter initialized — signing key cached");
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod() != null ? request.getMethod().name() : "";

        log.debug("JwtAuthFilter → [{} {}]", method, path);

        if (HttpMethod.OPTIONS.matches(method)) {
            return chain.filter(exchange);
        }

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or malformed Authorization header: {}", path);
            return rejectUnauthorized(exchange, "Missing Authorization header");
        }

        String token = authHeader.substring(7).trim();

        try {
            Claims claims = parseToken(token);

            String username = claims.getSubject();
            String role = claims.get("role", String.class);
            String userId = claims.get("userId", String.class);

            log.debug("JWT valid — user: {}, role: {}", username, role);

            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-Auth-User", username != null ? username : "")
                    .header("X-Auth-Roles", role != null ? role : "")
                    .header("X-Auth-UserId", userId != null ? userId : "")
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (ExpiredJwtException e) {
            log.warn("JWT expired: {}", path);
            return rejectUnauthorized(exchange, "Token expired");

        } catch (JwtException e) {
            log.warn("Invalid JWT: {} — {}", path, e.getMessage());
            return rejectUnauthorized(exchange, "Invalid token");

        } catch (Exception e) {
            log.error("JWT error: {}", path, e);
            return rejectUnauthorized(exchange, "Authentication error");
        }
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream()
                .anyMatch(publicPath ->
                        path.equals(publicPath) || path.startsWith(publicPath + "/"));
    }

    private Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(this.signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Mono<Void> rejectUnauthorized(ServerWebExchange exchange, String reason) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().set("X-Auth-Error", reason);
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE, "application/json");
        return response.setComplete();
    }
}