package com.example.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RedisConfig {
  @Bean
  KeyResolver simpleClientAddressResolver() {
    return exchange -> Mono.just(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
  }

  @Bean
  RedisRateLimiter redisRateLimiter() {
    return new RedisRateLimiter(1, 1); // replenish rate and burst capacity
  }
}
