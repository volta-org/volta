package com.volta.engine;

import com.volta.http.HttpSender;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadEngine {
  private static final Logger log = LoggerFactory.getLogger(LoadEngine.class);

  private final String url;
  private final int targetRps;
  private final int durationSeconds;
  private volatile boolean running = false;
  private static final int MAX_CONCURRENT_REQUESTS = 1000;

  public LoadEngine(String url, int targetRps, int durationSeconds) {
    if (url == null || url.isBlank()) {
      throw new IllegalArgumentException("URL must not be empty");
    }
    if (targetRps <= 0) {
      throw new IllegalArgumentException("RPS must be positive");
    }
    if (durationSeconds <= 0) {
      throw new IllegalArgumentException("Duration must be positive");
    }

    this.url = url;

    this.targetRps = targetRps;
    this.durationSeconds = durationSeconds;
  }

  public void start() {
    running = true;
    long intervalNanos = 1_000_000_000L / targetRps;
    long endTime = System.nanoTime() + (long) durationSeconds * 1_000_000_000L;
    long sendNextTime = System.nanoTime();

    Semaphore semaphore = new Semaphore(MAX_CONCURRENT_REQUESTS);

    try (HttpSender sender = new HttpSender();
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

      while (running && System.nanoTime() < endTime) {

        long waitMillis = (sendNextTime - System.nanoTime()) / 1_000_000;

        if (waitMillis > 0) {
          try {
            Thread.sleep(waitMillis);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
          }
        }

        try {
          semaphore.acquire();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }

        executor.submit(
            () -> {
              try {
                HttpResponse<String> response = sender.send(url);
                log.info("Status: {}, Body: {}", response.statusCode(), response.body());
              } catch (Exception e) {
                log.error("Request failed", e);
              } finally {
                semaphore.release();
              }
            });

        if (System.nanoTime() - sendNextTime > 1_000_000_000L) {
          sendNextTime = System.nanoTime();
        }

        sendNextTime += intervalNanos;
      }
    } catch (Exception e) {
      log.error("Sender closed or failed to initialize", e);
    }

    // try-with-resources handles executor.close() and sender.close()
    running = false;
    log.info("Test finished");
  }

  public void stop() {
    running = false;
  }
}
