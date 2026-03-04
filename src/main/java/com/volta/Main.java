package com.volta;

import java.net.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
  private static final Logger log = LoggerFactory.getLogger(Main.class);
  private static final String TARGET_URL = "https://jsonplaceholder.typicode.com/posts/1";

  public static void main(String[] args) {
    try (HttpSender sender = new HttpSender()) {
      while (true) {
        try {
          Thread.sleep(1000);
          HttpResponse<String> response = sender.send(TARGET_URL);
          log.info("Status: {}, Body: {}", response.statusCode(), response.body());
        } catch (Exception e) {
          log.error("Request failed", e);
        }
      }
    } catch (Exception e) {
      log.error("Sender closed or failed to initialize", e);
    }
  }
}
