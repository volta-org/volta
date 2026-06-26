package com.volta.stats;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class StatsAggregatorTest {

  @Test
  void mergeEmptyReturnsZeros() {
    StatsSnapshot merged = StatsAggregator.merge(List.of());

    assertEquals(0, merged.totalRequests());
    assertEquals(0, merged.successCount());
    assertEquals(0.0, merged.avgLatencyMs());
  }

  @Test
  void mergeSingleReturnsSameSnapshot() {
    StatsSnapshot snapshot = new StatsSnapshot(100, 95, 5, 50.0, 10, 200);

    StatsSnapshot merged = StatsAggregator.merge(List.of(snapshot));

    assertEquals(snapshot, merged);
  }

  @Test
  void mergeSumsCountsAndComputesWeightedAverage() {
    StatsSnapshot first = new StatsSnapshot(100, 90, 10, 100.0, 20, 300);
    StatsSnapshot second = new StatsSnapshot(200, 200, 0, 50.0, 10, 150);

    StatsSnapshot merged = StatsAggregator.merge(List.of(first, second));

    assertEquals(300, merged.totalRequests());
    assertEquals(290, merged.successCount());
    assertEquals(10, merged.errorCount());
    assertEquals(10, merged.minLatencyMs());
    assertEquals(300, merged.maxLatencyMs());
    assertEquals(66.66666666666667, merged.avgLatencyMs(), 0.0001);
  }

  @Test
  void mergeIgnoresEmptySnapshotsForMinMax() {
    StatsSnapshot empty = new StatsSnapshot(0, 0, 0, 0.0, 0, 0);
    StatsSnapshot active = new StatsSnapshot(50, 50, 0, 40.0, 15, 120);

    StatsSnapshot merged = StatsAggregator.merge(List.of(empty, active));

    assertEquals(50, merged.totalRequests());
    assertEquals(15, merged.minLatencyMs());
    assertEquals(120, merged.maxLatencyMs());
  }
}
