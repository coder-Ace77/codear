package com.codear.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class SimpleFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(SimpleFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        log.info("🌐 Gateway received request: [{}] {}", path);

        exchange.getResponse().getHeaders().add("X-Gateway", "Request passed through gateway");

        return chain.filter(exchange)
                .then(Mono.fromRunnable(() ->
                        log.info("✅ Response sent for [{}] {}", path)
                ));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}