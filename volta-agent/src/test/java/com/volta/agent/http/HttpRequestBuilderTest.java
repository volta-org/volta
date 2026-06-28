package com.volta.agent.http;

import static org.junit.jupiter.api.Assertions.*;

import com.volta.model.HttpMethod;
import com.volta.model.RequestSpec;
import java.net.http.HttpRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HttpRequestBuilderTest {

  @Test
  void buildsGetRequestWithoutBody() {
    RequestSpec spec = RequestSpec.get("https://example.com/items");

    HttpRequest request = HttpRequestBuilder.build(spec);

    assertEquals("GET", request.method());
    assertEquals("https://example.com/items", request.uri().toString());
  }

  @Test
  void buildsPostRequestWithHeadersAndJsonBody() {
    RequestSpec spec =
        new RequestSpec(
            HttpMethod.POST,
            "https://example.com/api",
            Map.of("Content-Type", "application/json", "X-Test", "volta"),
            "{\"name\":\"volta\"}");

    HttpRequest request = HttpRequestBuilder.build(spec);

    assertEquals("POST", request.method());
    assertEquals("application/json", request.headers().firstValue("Content-Type").orElseThrow());
    assertEquals("volta", request.headers().firstValue("X-Test").orElseThrow());
    assertTrue(request.bodyPublisher().isPresent());
  }

  @Test
  void buildsPutRequest() {
    RequestSpec spec =
        new RequestSpec(
            HttpMethod.PUT,
            "https://example.com/resource/1",
            Map.of("Content-Type", "application/json"),
            "{\"status\":\"ok\"}");

    HttpRequest request = HttpRequestBuilder.build(spec);

    assertEquals("PUT", request.method());
  }

  @Test
  void rejectsBodyForDelete() {
    RequestSpec spec =
        new RequestSpec(HttpMethod.DELETE, "https://example.com/resource/1", Map.of(), "oops");

    IllegalArgumentException error =
        assertThrows(IllegalArgumentException.class, () -> HttpRequestBuilder.build(spec));

    assertTrue(error.getMessage().contains("Body is not allowed"));
  }
}
