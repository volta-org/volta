package com.volta.agent.engine;

import com.volta.agent.http.HttpSender;
import com.volta.model.RequestSpec;
import com.volta.stats.StatsCollector;
import com.volta.stats.StatsSnapshot;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadEngine {
  private static final Logger log = LoggerFactory.getLogger(LoadEngine.class);
  private final StatsCollector collector = new StatsCollector();

  private final RequestSpec request;
  private volatile int targetRps;
  private final int durationSeconds;
  private volatile boolean running = false;
  private static final int MAX_CONCURRENT_REQUESTS = 1000;

  public LoadEngine(RequestSpec request, int targetRps, int durationSeconds) {
    if (request == null) {
      throw new IllegalArgumentException("Request must not be null");
    }
    if (targetRps <= 0) {
      throw new IllegalArgumentException("RPS must be positive");
    }
    if (durationSeconds <= 0) {
      throw new IllegalArgumentException("Duration must be positive");
    }

    this.request = request;
    this.targetRps = targetRps;
    this.durationSeconds = durationSeconds;
  }

  public void updateTargetRps(int newRps) {
    if (newRps <= 0) {
      throw new IllegalArgumentException("RPS must be positive");
    }
    this.targetRps = newRps;
  }

  public void start() {
    running = true;
    long endTime;
    long sendNextTime;

    Semaphore semaphore = new Semaphore(MAX_CONCURRENT_REQUESTS);

    try (HttpSender sender = new HttpSender();
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

      endTime = System.nanoTime() + (long) durationSeconds * 1_000_000_000L;
      sendNextTime = System.nanoTime();

      while (running && System.nanoTime() < endTime) {
        long intervalNanos = 1_000_000_000L / targetRps;

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
              long startNano = System.nanoTime();
              try {
                HttpResponse<String> response = sender.send(request);
                long latencyMs = (System.nanoTime() - startNano) / 1_000_000;

                collector.record(response.statusCode(), latencyMs);
              } catch (Exception e) {
                long latencyMs = (System.nanoTime() - startNano) / 1_000_000;

                collector.record(503, latencyMs);
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

    running = false;
    log.info("Test finished");
  }

  public StatsSnapshot getStats() {
    return collector.getSnapshot();
  }

  public StatsSnapshot getStatsAndReset() {
    return collector.getSnapshotAndReset();
  }

  public void stop() {
    running = false;
  }
}
