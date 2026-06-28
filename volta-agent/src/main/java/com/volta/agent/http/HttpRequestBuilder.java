package com.volta.agent.http;

import com.volta.model.HttpMethod;
import com.volta.model.RequestSpec;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;

public final class HttpRequestBuilder {

  private HttpRequestBuilder() {}

  public static HttpRequest build(RequestSpec spec) {
    HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(spec.url()));
    spec.headers().forEach(builder::header);

    HttpMethod method = spec.method();
    if (method.allowsBody()) {
      String body = spec.body() == null ? "" : spec.body();
      builder.method(
          method.name(), HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
      return builder.build();
    }

    if (spec.body() != null && !spec.body().isBlank()) {
      throw new IllegalArgumentException("Body is not allowed for " + method);
    }

    return switch (method) {
      case GET -> builder.GET().build();
      case DELETE -> builder.DELETE().build();
      default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
    };
  }
}
