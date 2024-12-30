package com.example.gateway.filter;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.example.gateway.config.GatewayCacheProperties;

import reactor.core.publisher.Mono;

@Component
public class CaffeineCacheGatewayFilter extends AbstractGatewayFilterFactory<CaffeineCacheGatewayFilter.Config> {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CaffeineCacheGatewayFilter.class);

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private GatewayCacheProperties cacheProperties;

    @Autowired
    private ModifyResponseBodyGatewayFilterFactory modifyResponseBodyFilter;

    public CaffeineCacheGatewayFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new OrderedGatewayFilter((exchange, chain) -> {
            String cacheKey = generateCacheKey(exchange);
            logger.debug("Checking cache for key: {}", cacheKey);

            org.springframework.cache.Cache cache = cacheManager.getCache("gateway-cache");
            String cachedResponse = cache != null ? cache.get(cacheKey, String.class) : null;

            if (cachedResponse != null) {
                logger.debug("Cache hit for key: {}", cacheKey);
                return writeResponse(exchange, cachedResponse);
            }

            logger.debug("Cache miss for key: {}", cacheKey);

            ModifyResponseBodyGatewayFilterFactory.Config modifyConfig = new ModifyResponseBodyGatewayFilterFactory.Config()
                    .setRewriteFunction(String.class, String.class, (webExchange, responseBody) -> {
                        logger.debug("Modifying response body");

                        if (responseBody != null && shouldCache(webExchange)) {
                            try {
                                cache.put(cacheKey, responseBody);
                                logger.debug("Successfully cached response for key: {}", cacheKey);
                            } catch (Exception e) {
                                logger.error("Error caching response: ", e);
                            }
                        }

                        return Mono.just(responseBody);
                    });

            GatewayFilter modifyFilter = modifyResponseBodyFilter.apply(modifyConfig);
            return modifyFilter.filter(exchange, chain);
        }, Ordered.HIGHEST_PRECEDENCE);
    }

    private boolean shouldCache(ServerWebExchange exchange) {
        HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
        MediaType contentType = exchange.getResponse().getHeaders().getContentType();

        boolean isSuccess = statusCode != null && statusCode.is2xxSuccessful();
        boolean isJson = contentType != null && contentType.includes(MediaType.APPLICATION_JSON);

        logger.debug("Should cache check - Status: {}, ContentType: {}, IsSuccess: {}, IsJson: {}",
                statusCode, contentType, isSuccess, isJson);

        return isSuccess && isJson;
    }

    private String generateCacheKey(ServerWebExchange exchange) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String routeId = (route != null) ? route.getId() : "unknown";
        String path = exchange.getRequest().getPath().value();
        String query = exchange.getRequest().getURI().getRawQuery();
        String key = routeId + ":" + path + (query != null ? "?" + query : "");
        logger.debug("Generated cache key: {}", key);
        return key;
    }

    private Mono<Void> writeResponse(ServerWebExchange exchange, String cachedResponse) {
        try {
            logger.debug("Writing cached response of length: {}", cachedResponse.length());
            byte[] bytes = cachedResponse.getBytes(StandardCharsets.UTF_8);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception e) {
            logger.error("Error writing cached response: ", e);
            return Mono.error(e);
        }
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Collections.emptyList();
    }

    public static class Config {
    }
}