package com.volta.master.reporter;

import com.volta.master.StartupRunner;
import com.volta.master.client.AgentClient;
import com.volta.stats.StatsSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StatsReporter {
  private static final Logger log = LoggerFactory.getLogger(StartupRunner.class);

  // ANSI-codes for colors
  public static final String RESET = "\u001B[0m";
  public static final String GREEN = "\u001B[32m";
  public static final String RED = "\u001B[31m";
  public static final String YELLOW = "\u001B[33m";
  public static final String BLUE = "\u001B[34m";
  public static final String CYAN = "\u001B[36m";
  public static final String BOLD = "\u001B[1m";

  private final AgentClient agentClient;

  public StatsReporter(AgentClient agentClient) {
    this.agentClient = agentClient;
  }

  public void startReporting(String agentUrl, int durationSeconds) {
    log.info("Starting live stats reporting for {}s", durationSeconds);

    for (int i = 0; i < durationSeconds; i++) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.warn("Stats reporting interrupted");
        break;
      }

      try {
        StatsSnapshot stats = agentClient.getStats(agentUrl);
        printLiveStats(stats, i + 1);
      } catch (Exception e) {
        log.error("Failed to fetch stats: {}", e.getMessage());
      }
    }

    printFinalStats(agentUrl);
  }

  private void printLiveStats(StatsSnapshot stats, int elapsedSeconds) {
    double successRate = calculateSuccessRate(stats);
    long currentRps = calculateCurrentRps(stats, elapsedSeconds);

    String rateColor = (successRate >= 100.0) ? GREEN : (successRate > 95.0 ? YELLOW : RED);
    String errorColor = (stats.errorCount() > 0) ? RED : RESET;

    String line =
        String.format(
            "[%sRPS: %d%s | %sSuccess: %.1f%%%s | %sAvg: %.0fms%s | %sErrors: %d%s]",
            CYAN,
            currentRps,
            RESET,
            rateColor,
            successRate,
            RESET,
            BLUE,
            stats.avgLatencyMs(),
            RESET,
            errorColor,
            stats.errorCount(),
            RESET);

    System.out.println(line);
  }

  private void printFinalStats(String agentUrl) {
    try {
      StatsSnapshot finalStats = agentClient.getStats(agentUrl);

      System.out.println("\n" + BOLD + CYAN + "========= FINAL STATS =========" + RESET);

      System.out.printf(
          "Total Requests:  %s%s%d%s\n", BOLD, BLUE, finalStats.totalRequests(), RESET);
      System.out.printf(
          "Success:         %s%s%d%s\n", BOLD, GREEN, finalStats.successCount(), RESET);
      System.out.printf("Errors:          %s%s%d%s\n", BOLD, RED, finalStats.errorCount(), RESET);

      System.out.printf(
          "Success Rate:    %s%.2f%%%s\n", BOLD, calculateSuccessRate(finalStats), RESET);
      System.out.printf(
          "Avg Latency:     %s%s%.2fms%s\n", BOLD, YELLOW, finalStats.avgLatencyMs(), RESET);

      System.out.printf("Min Latency:     %dms\n", finalStats.minLatencyMs());
      System.out.printf("Max Latency:     %dms\n", finalStats.maxLatencyMs());

      System.out.println(BOLD + CYAN + "===============================" + RESET);
    } catch (Exception e) {
      log.error("Failed to fetch final stats: {}", e.getMessage());
    }
  }

  private double calculateSuccessRate(StatsSnapshot stats) {
    if (stats.totalRequests() == 0) {
      return 0.0;
    }
    return (stats.successCount() * 100.0) / stats.totalRequests();
  }

  private long calculateCurrentRps(StatsSnapshot stats, int elapsedSeconds) {
    if (elapsedSeconds == 0) {
      return 0;
    }
    return stats.totalRequests() / elapsedSeconds;
  }
}
