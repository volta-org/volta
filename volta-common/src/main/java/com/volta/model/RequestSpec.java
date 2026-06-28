package com.volta.model;

import java.util.Map;

public record RequestSpec(HttpMethod method, String url, Map<String, String> headers, String body) {

  public RequestSpec {
    method = method == null ? HttpMethod.GET : method;
    headers = headers == null || headers.isEmpty() ? Map.of() : Map.copyOf(headers);
  }

  public static RequestSpec get(String url) {
    return new RequestSpec(HttpMethod.GET, url, Map.of(), null);
  }
}
