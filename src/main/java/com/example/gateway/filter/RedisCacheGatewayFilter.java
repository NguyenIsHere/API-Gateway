package com.example.gateway.filter;

import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.example.gateway.config.GatewayCacheProperties;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class RedisCacheGatewayFilter extends AbstractGatewayFilterFactory<RedisCacheGatewayFilter.Config> {

    @Autowired
    @Qualifier("reactiveRedisTemplate")
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Autowired
    private GatewayCacheProperties cacheProperties;

    public RedisCacheGatewayFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String cacheKey = exchange.getRequest().getURI().toString();

            return redisTemplate.opsForValue().get(cacheKey)
                    .flatMap(cachedResponse -> writeResponse(exchange, cachedResponse))
                    .switchIfEmpty(
                            chain.filter(exchange).then(Mono.defer(() -> cacheResponse(exchange, cacheKey, chain))));
        };
    }

    private Mono<Void> writeResponse(ServerWebExchange exchange, String cachedResponse) {
        byte[] bytes = cachedResponse.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private Mono<Void> cacheResponse(ServerWebExchange exchange, String cacheKey, GatewayFilterChain chain) {
        String routeId = exchange.getAttributeOrDefault(
                "org.springframework.cloud.gateway.support.ServerWebExchangeUtils.gatewayRouteId", "default-route");
        Long ttl = cacheProperties.getServices().getOrDefault(routeId, 300L);

        ServerHttpResponseDecorator responseDecorator = new ServerHttpResponseDecorator(exchange.getResponse()) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                Flux<? extends DataBuffer> bodyFlux = Flux.from(body);
                return super.writeWith(
                        bodyFlux.map(dataBuffer -> {
                            byte[] bytes = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(bytes);
                            String bodyString = new String(bytes, StandardCharsets.UTF_8);
                            // Loại bỏ header Cache-Control trước khi cache
                            exchange.getResponse().getHeaders().remove(HttpHeaders.CACHE_CONTROL);
                            // Add logging for debugging
                            System.out.println("Attempting to cache response for key: " + cacheKey);
                            redisTemplate.opsForValue().set(cacheKey, bodyString, Duration.ofSeconds(ttl))
                                    .doOnSuccess(v -> System.out.println("Cached response for key: " + cacheKey))
                                    .doOnError(e -> System.err.println("Failed to cache response: " + e.getMessage()))
                                    .subscribe();

                            return exchange.getResponse().bufferFactory().wrap(bytes);
                        }));
            }
        };

        return chain.filter(exchange.mutate().response(responseDecorator).build());
    }

    public static class Config {
    }
}
