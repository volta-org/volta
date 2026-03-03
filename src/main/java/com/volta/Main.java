package com.volta;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
  private static final Logger log = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {
    WireMockServer wireMockServer = new WireMockServer(8089);
    wireMockServer.start();

    wireMockServer.stubFor(get("/test").willReturn(ok("Hello from mock!")));

    try (HttpSender sender = new HttpSender()) {
      while (true) {
        try {
          Thread.sleep(1000);
          HttpResponse<String> response = sender.send("http://localhost:8089/test");
          log.info("Status: {}, Body: {}", response.statusCode(), response.body());
        } catch (Exception e) {
          log.error("Request failed", e);
        }
      }
    } catch (Exception e) {
      log.error("Sender closed or failed to initialize", e);
    }

    wireMockServer.stop();
  }
}
