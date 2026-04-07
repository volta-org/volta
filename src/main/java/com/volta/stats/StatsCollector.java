package com.volta.stats;

import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;

public class StatsCollector {
  private final LongAdder totalRequests = new LongAdder();
  private final LongAdder successCount = new LongAdder();
  private final LongAdder totalLatencyMs = new LongAdder();
  private final LongAccumulator maxLatencyMs = new LongAccumulator(Math::max, Long.MIN_VALUE);
  private final LongAccumulator minLatencyMs = new LongAccumulator(Math::min, Long.MAX_VALUE);

  public void record(int statusCode, long latencyMs) {
    if (statusCode < 100 || statusCode > 599) {
      throw new IllegalArgumentException("statusCode must be integer in [100, 599]");
    }
    if (latencyMs < 0) {
      throw new IllegalArgumentException("latencyMs must be non-negative");
    }

    totalRequests.increment();
    if (200 <= statusCode && statusCode < 300) {
      successCount.increment();
    }
    totalLatencyMs.add(latencyMs);
    maxLatencyMs.accumulate(latencyMs);
    minLatencyMs.accumulate(latencyMs);
  }

  public StatsSnapshot getSnapshot() {
    long snapshotTotalRequests = totalRequests.sum();
    long snapshotSuccessCount = successCount.sum();
    long snapshotTotalLatencyMs = totalLatencyMs.sum();

    long snapshotMinLatencyMs = (snapshotTotalRequests == 0 ? 0 : minLatencyMs.get());
    long snapshotMaxLatencyMs = (snapshotTotalRequests == 0 ? 0 : maxLatencyMs.get());
    double snapshotAvgLatencyMs =
        (snapshotTotalRequests == 0
            ? 0.0
            : (double) snapshotTotalLatencyMs / snapshotTotalRequests);

    return new StatsSnapshot(
        snapshotTotalRequests,
        snapshotSuccessCount,
        Math.max(snapshotTotalRequests - snapshotSuccessCount, 0),
        snapshotAvgLatencyMs,
        snapshotMinLatencyMs,
        snapshotMaxLatencyMs);
  }

  /**
   * Returns a snapshot of current stats and resets all counters.
   *
   * <p>Must be called only after all recording threads have finished. Not safe to call concurrently
   * with {@link #record}.
   */
  public StatsSnapshot getSnapshotAndReset() {
    StatsSnapshot snapshot = getSnapshot();
    reset();
    return snapshot;
  }

  public void reset() {
    totalRequests.reset();
    successCount.reset();
    totalLatencyMs.reset();
    minLatencyMs.reset();
    maxLatencyMs.reset();
  }
}
