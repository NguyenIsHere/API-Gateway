package com.example.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ModifyResponseHeadersFilter extends AbstractGatewayFilterFactory<ModifyResponseHeadersFilter.Config>
    implements Ordered {

  public ModifyResponseHeadersFilter() {
    super(Config.class);
  }

  @Override
  public GatewayFilter apply(Config config) {
    return (exchange, chain) -> chain.filter(exchange).then(Mono.fromRunnable(() -> {
      ServerHttpResponse response = exchange.getResponse();
      HttpHeaders headers = response.getHeaders();

      // Loại bỏ các header ngăn cản caching
      headers.remove(HttpHeaders.PRAGMA);
      headers.remove(HttpHeaders.EXPIRES);

      // Thiết lập lại Cache-Control nếu cần
      headers.setCacheControl("public, max-age=600");
    }));
  }

  @Override
  public int getOrder() {
    return -1; // Đảm bảo filter này chạy sau CaffeineCacheGatewayFilter
  }

  public static class Config {
    // Configuration properties nếu cần
  }
}
