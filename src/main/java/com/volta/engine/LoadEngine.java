package com.volta.engine;

import com.volta.http.HttpSender;
import java.net.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadEngine {
  private static final Logger log = LoggerFactory.getLogger(LoadEngine.class);

  private final String url;
  private final int targetRps;
  private final int durationSeconds;
  private volatile boolean running = false;

  public LoadEngine(String URL, int targetRPS, int durationSeconds) {
    this.url = URL;
    this.targetRps = targetRPS;
    this.durationSeconds = durationSeconds;
  }

  public void start() {
    running = true;
    long intervalNanos = 1_000_000_000L / targetRps;
    long endTime = System.nanoTime() + (long) durationSeconds * 1_000_000_000L;
    long sendNextTime = System.nanoTime();

    try (HttpSender sender = new HttpSender()) {
      while (running && System.nanoTime() < endTime) {

        while (System.nanoTime() < sendNextTime) {
          // busy-wait
        }

        try {
          HttpResponse<String> response = sender.send(url);
          log.info("Status: {}, Body: {}", response.statusCode(), response.body());
        } catch (Exception e) {
          log.error("Request failed", e);
        }
        sendNextTime += intervalNanos;
      }
    } catch (Exception e) {
      log.error("Sender closed or failed to initialize", e);
    }

    log.info("Test finished");
  }

  public void stop() {
    running = false;
  }
}
