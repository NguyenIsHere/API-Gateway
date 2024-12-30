package com.example.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Primary;

import java.util.HashMap;
import java.util.Map;

@Primary
@ConfigurationProperties(prefix = "gateway.cache.ttl")
public class GatewayCacheProperties {

  private Map<String, Long> services = new HashMap<>();

  public Map<String, Long> getServices() {
    return services;
  }

  public void setServices(Map<String, Long> services) {
    this.services = services;
  }

  private Long defaultTtl = 300L; // Default TTL value

  public Long getDefaultTtl() {
    return defaultTtl;
  }

  public void setDefaultTtl(Long defaultTtl) {
    this.defaultTtl = defaultTtl;
  }

}
