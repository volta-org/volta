package com.volta.stats;

import java.util.Collection;

public final class StatsAggregator {

  private StatsAggregator() {}

  public static StatsSnapshot merge(Collection<StatsSnapshot> snapshots) {
    if (snapshots.isEmpty()) {
      return empty();
    }
    if (snapshots.size() == 1) {
      return snapshots.iterator().next();
    }

    long totalRequests = 0;
    long successCount = 0;
    long errorCount = 0;
    long minLatency = Long.MAX_VALUE;
    long maxLatency = 0;
    double weightedLatencySum = 0;

    for (StatsSnapshot snapshot : snapshots) {
      totalRequests += snapshot.totalRequests();
      successCount += snapshot.successCount();
      errorCount += snapshot.errorCount();

      if (snapshot.totalRequests() > 0) {
        weightedLatencySum += snapshot.avgLatencyMs() * snapshot.totalRequests();
        minLatency = Math.min(minLatency, snapshot.minLatencyMs());
        maxLatency = Math.max(maxLatency, snapshot.maxLatencyMs());
      }
    }

    double avgLatency = totalRequests > 0 ? weightedLatencySum / totalRequests : 0.0;
    if (minLatency == Long.MAX_VALUE) {
      minLatency = 0;
    }

    return new StatsSnapshot(
        totalRequests, successCount, errorCount, avgLatency, minLatency, maxLatency);
  }

  private static StatsSnapshot empty() {
    return new StatsSnapshot(0, 0, 0, 0.0, 0, 0);
  }
}
