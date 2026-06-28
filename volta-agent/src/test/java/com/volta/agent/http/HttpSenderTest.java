package com.volta.agent.http;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.volta.model.HttpMethod;
import com.volta.model.RequestSpec;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;

@WireMockTest(httpPort = 8089)
class HttpSenderTest {

  private final HttpSender sender = new HttpSender();

  @Test
  void sendReturns200ForValidUrl() throws Exception {
    stubFor(get("/test").willReturn(ok("Hello from mock!")));

    HttpResponse<String> response = sender.send("http://localhost:8089/test");
    assertEquals(200, response.statusCode());
  }

  @Test
  void responseBodyIsNotEmpty() throws Exception {
    stubFor(get("/test").willReturn(ok("Hello from mock!")));

    HttpResponse<String> response = sender.send("http://localhost:8089/test");
    assertFalse(response.body().isEmpty());
  }

  @Test
  void sendPostWithJsonBodyReturns200() throws Exception {
    stubFor(
        post("/api")
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(equalToJson("{\"name\":\"volta\"}"))
            .willReturn(ok("created")));

    RequestSpec spec =
        new RequestSpec(
            HttpMethod.POST,
            "http://localhost:8089/api",
            Map.of("Content-Type", "application/json"),
            "{\"name\":\"volta\"}");

    HttpResponse<String> response = sender.send(spec);

    assertEquals(200, response.statusCode());
    assertEquals("created", response.body());
  }

  @Test
  void sendThrowsOnInvalidUrl() {
    assertThrows(Exception.class, () -> sender.send("http://localhost:1"));
  }
}
