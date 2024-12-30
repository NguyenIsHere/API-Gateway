package com.example.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@EnableCaching
@ConfigurationProperties(prefix = "gateway.cache")
@Component
public class GatewayCacheProperties {
  private Ttl ttl = new Ttl();

  public static class Ttl {
    private Integer defaultValue = 600;
    private Map<String, Integer> services = new HashMap<>();

    public Integer getDefaultValue() {
      return defaultValue;
    }

    public void setDefaultValue(Integer defaultValue) {
      this.defaultValue = defaultValue;
    }

    public Map<String, Integer> getServices() {
      return services;
    }

    public void setServices(Map<String, Integer> services) {
      this.services = services;
    }
  }

  public Ttl getTtl() {
    return ttl;
  }

  public void setTtl(Ttl ttl) {
    this.ttl = ttl;
  }
}
