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

    String line =
        String.format(
            "[RPS: %d | Success: %.1f%% | Avg: %.0fms | Errors: %d]",
            currentRps, successRate, stats.avgLatencyMs(), stats.errorCount());

    System.out.println(line);
  }

  private void printFinalStats(String agentUrl) {
    try {
      StatsSnapshot finalStats = agentClient.getStats(agentUrl);

      System.out.println("\n========= FINAL STATS =========");
      System.out.println("Total Requests:  " + finalStats.totalRequests());
      System.out.println("Success:         " + finalStats.successCount());
      System.out.println("Errors:          " + finalStats.errorCount());
      System.out.printf("Success Rate:    %.2f%%\n", calculateSuccessRate(finalStats));
      System.out.printf("Avg Latency:     %.2fms\n", finalStats.avgLatencyMs());
      System.out.printf("Min Latency:     %dms\n", finalStats.minLatencyMs());
      System.out.printf("Max Latency:     %dms\n", finalStats.maxLatencyMs());
      System.out.println("===============================");
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
