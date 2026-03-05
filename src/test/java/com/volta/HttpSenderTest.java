package com.volta;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.http.HttpResponse;
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
  void sendThrowsOnInvalidUrl() {
    assertThrows(Exception.class, () -> sender.send("http://localhost:1"));
  }
}
