package com.codear.gateway.filter;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List; 
import java.util.ArrayList; 

@Component
public class JwtAuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<JwtAuthenticationGatewayFilterFactory.Config> {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationGatewayFilterFactory.class);
    private static final String HARDCODED_SECRET = "Y29sb3JibHVlc2VsZWN0aW9ubXVzdGZpZ2h0bGF1Z2hhcHBlYXJhbmNldmlsbGFnZXA=";
    
    private final Key signingKey;

    public JwtAuthenticationGatewayFilterFactory() {
        super(Config.class);
        byte[] keyBytes = Base64.getDecoder().decode(HARDCODED_SECRET.getBytes());
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath(); 
            logger.debug("Applying JWT filter to request: {}", request.getURI());

            boolean isExcluded = config.getExcludedPaths().stream().anyMatch(path::equals);

            if (isExcluded) {
                logger.debug("Path is excluded from JWT validation: {}", path);
                return chain.filter(exchange);
            }

            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                logger.warn("Missing Authorization header for path: {}", path);
                return onError(exchange, "Missing Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getOrEmpty(HttpHeaders.AUTHORIZATION).get(0);
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.warn("Invalid Authorization header format for path: {}", path);
                return onError(exchange, "Invalid Authorization header format", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            try {
                if (!isTokenValid(token)) {
                    logger.warn("Invalid JWT token for path: {}", path);
                    return onError(exchange, "Invalid JWT token", HttpStatus.UNAUTHORIZED);
                }
            } catch (Exception e) {
                logger.error("Token validation error for path: {}: {}", path, e.getMessage());
                return onError(exchange, "Token validation error", HttpStatus.UNAUTHORIZED);
            }

            logger.debug("JWT token is valid. Proceeding with filter chain for path: {}", path);
            return chain.filter(exchange);
        };
    }

    private boolean isTokenValid(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token);
            
            return true; 
        } catch (SignatureException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false; 
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        logger.warn("Error processing request: {} - Status: {}", err, httpStatus);
        
        return response.setComplete();
    }

    // --- UPDATED: Config class now accepts excluded paths ---
    public static class Config {
        private List<String> excludedPaths = new ArrayList<>();
        public List<String> getExcludedPaths() {
            return excludedPaths;
        }

        public void setExcludedPaths(List<String> excludedPaths) {
            this.excludedPaths = excludedPaths;
        }
    }
}