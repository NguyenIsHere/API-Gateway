package com.example.gateway.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@EnableCaching
@Configuration
public class CaffeineCacheConfig {

  @Bean
  public Caffeine<Object, Object> caffeineConfig() {
    return Caffeine.newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES) // TTL mặc định 10 phút
        .maximumSize(1000); // Giới hạn số lượng key
  }

  @Bean
  public CacheManager cacheManager(Caffeine<Object, Object> caffeine) {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager("gateway-cache");
    cacheManager.setCaffeine(caffeine);
    return cacheManager;
  }
}
