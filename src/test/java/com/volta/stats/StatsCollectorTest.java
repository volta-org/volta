package com.volta.stats;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class StatsCollectorTest {

  private StatsCollector collector;

  @BeforeEach
  void setUp() {
    collector = new StatsCollector();
  }

  @Nested
  class Record {

    @Test
    void throwsOnNegativeStatusCode() {
      assertThrows(IllegalArgumentException.class, () -> collector.record(-1, 100));
    }

    @Test
    void throwsOnStatusCodeBelow100() {
      assertThrows(IllegalArgumentException.class, () -> collector.record(99, 100));
    }

    @Test
    void throwsOnStatusCodeAbove599() {
      assertThrows(IllegalArgumentException.class, () -> collector.record(600, 100));
    }

    @Test
    void throwsOnNegativeLatency() {
      assertThrows(IllegalArgumentException.class, () -> collector.record(200, -1));
    }

    @Test
    void boundaryStatusCodes() {
      record Response(Integer statusCode, Integer latencyMs) {}

      List<Response> responses =
          List.of(
              new Response(200, 10),
              new Response(299, 10),
              new Response(199, 10),
              new Response(300, 10));

      int totalRequests = 0;
      int successCount = 0;
      int errorCount = 0;
      for (var response : responses) {
        collector.record(response.statusCode, response.latencyMs);

        totalRequests++;
        if (200 <= response.statusCode && response.statusCode < 300) {
          successCount++;
        } else {
          errorCount++;
        }

        StatsSnapshot snapshot = collector.getSnapshot();
        assertEquals(
            totalRequests,
            snapshot.totalRequests(),
            "on response (" + response.statusCode + ", " + response.latencyMs + ")");
        assertEquals(
            successCount,
            snapshot.successCount(),
            "on response (" + response.statusCode + ", " + response.latencyMs + ")");
        assertEquals(
            errorCount,
            snapshot.errorCount(),
            "on response (" + response.statusCode + ", " + response.latencyMs + ")");
      }
    }

    @Test
    void zeroLatencyIsValid() {
      assertDoesNotThrow(() -> collector.record(200, 0));
      StatsSnapshot snapshot = collector.getSnapshot();
      assertEquals(1, snapshot.totalRequests());
      assertEquals(0, snapshot.minLatencyMs());
    }
  }

  @Nested
  class GetSnapshot {

    @Test
    void beforeAnyRecordsReturnsAllZeros() {
      StatsSnapshot snapshot = collector.getSnapshot();
      assertEquals(0, snapshot.totalRequests());
      assertEquals(0, snapshot.successCount());
      assertEquals(0, snapshot.errorCount());
      assertEquals(0, snapshot.avgLatencyMs(), 0.01);
      assertEquals(0, snapshot.minLatencyMs());
      assertEquals(0, snapshot.maxLatencyMs());
    }
  }

  @Nested
  class RecordAndGetSnapshot {

    @Test
    void singleSuccessRecordCountedCorrectly() {
      collector.record(200, 50);

      StatsSnapshot snapshot = collector.getSnapshot();
      assertEquals(1, snapshot.totalRequests());
      assertEquals(1, snapshot.successCount());
      assertEquals(0, snapshot.errorCount());
      assertEquals(50, snapshot.avgLatencyMs(), 0.01);
      assertEquals(50, snapshot.minLatencyMs());
      assertEquals(50, snapshot.maxLatencyMs());
    }

    @Test
    void singleErrorRecordCountedCorrectly() {
      collector.record(500, 100);

      StatsSnapshot snapshot = collector.getSnapshot();
      assertEquals(1, snapshot.totalRequests());
      assertEquals(0, snapshot.successCount());
      assertEquals(1, snapshot.errorCount());
      assertEquals(100, snapshot.minLatencyMs());
      assertEquals(100, snapshot.avgLatencyMs(), 0.01);
      assertEquals(100, snapshot.maxLatencyMs());
    }

    @Test
    void multipleRecordsAllCountersCorrect() {
      collector.record(200, 10);
      collector.record(200, 20);
      collector.record(500, 30);
      collector.record(404, 40);

      StatsSnapshot snapshot = collector.getSnapshot();
      assertEquals(4, snapshot.totalRequests());
      assertEquals(2, snapshot.successCount());
      assertEquals(2, snapshot.errorCount());
      assertEquals(10, snapshot.minLatencyMs());
      assertEquals(25, snapshot.avgLatencyMs(), 0.01);
      assertEquals(40, snapshot.maxLatencyMs());
    }

    @Test
    void averageLatencyCalculatedCorrectly() {
      collector.record(200, 10);
      collector.record(200, 20);
      collector.record(200, 30);

      StatsSnapshot snapshot = collector.getSnapshot();
      assertEquals(20, snapshot.avgLatencyMs(), 0.01);
    }

    @Test
    void minLatencyTrackedCorrectly() {
      collector.record(200, 50);
      collector.record(200, 10);
      collector.record(200, 30);

      StatsSnapshot snapshot = collector.getSnapshot();
      assertEquals(10, snapshot.minLatencyMs());
    }

    @Test
    void maxLatencyTrackedCorrectly() {
      collector.record(200, 10);
      collector.record(200, 50);
      collector.record(200, 30);

      StatsSnapshot snapshot = collector.getSnapshot();
      assertEquals(50, snapshot.maxLatencyMs());
    }
  }

  @Nested
  class RecordGetSnapshotAndReset {

    @Test
    void recordsAfterResetAreIndependentOfPreviousState() {
      collector.record(200, 100);
      collector.record(500, 200);
      collector.reset();

      collector.record(200, 50);

      StatsSnapshot snapshot = collector.getSnapshot();
      assertEquals(1, snapshot.totalRequests());
      assertEquals(1, snapshot.successCount());
      assertEquals(0, snapshot.errorCount());
      assertEquals(50, snapshot.avgLatencyMs(), 0.01);
      assertEquals(50, snapshot.minLatencyMs());
      assertEquals(50, snapshot.maxLatencyMs());
    }

    @Test
    void tenCyclesWithRandomData() {
      var random = new java.util.Random(42);

      for (int cycle = 0; cycle < 10; cycle++) {
        int recordCount = random.nextInt(10) + 1;

        long expectedTotal = 0;
        long expectedSuccess = 0;
        long expectedErrors = 0;
        long expectedTotalLatency = 0;
        long expectedMin = Long.MAX_VALUE;
        long expectedMax = 0;

        for (int j = 0; j < recordCount; j++) {
          int statusCode = random.nextInt(500) + 100;
          long latencyMs = random.nextInt(1000);

          collector.record(statusCode, latencyMs);

          expectedTotal++;
          if (200 <= statusCode && statusCode < 300) {
            expectedSuccess++;
          } else {
            expectedErrors++;
          }
          expectedTotalLatency += latencyMs;
          expectedMin = Math.min(expectedMin, latencyMs);
          expectedMax = Math.max(expectedMax, latencyMs);
        }

        StatsSnapshot snapshot = collector.getSnapshotAndReset();
        double expectedAvg = (double) expectedTotalLatency / expectedTotal;

        assertEquals(expectedTotal, snapshot.totalRequests(), "cycle " + cycle + ": totalRequests");
        assertEquals(expectedSuccess, snapshot.successCount(), "cycle " + cycle + ": successCount");
        assertEquals(expectedErrors, snapshot.errorCount(), "cycle " + cycle + ": errorCount");
        assertEquals(expectedAvg, snapshot.avgLatencyMs(), 0.01, "cycle " + cycle + ": avgLatency");
        assertEquals(expectedMin, snapshot.minLatencyMs(), "cycle " + cycle + ": minLatency");
        assertEquals(expectedMax, snapshot.maxLatencyMs(), "cycle " + cycle + ": maxLatency");
      }
    }

    @Test
    void doubleResetIsSafe() {
      collector.record(200, 50);
      collector.reset();
      assertDoesNotThrow(() -> collector.reset());

      StatsSnapshot snapshot = collector.getSnapshot();
      assertEquals(0, snapshot.totalRequests());
      assertEquals(0, snapshot.successCount());
      assertEquals(0, snapshot.errorCount());
      assertEquals(0, snapshot.avgLatencyMs(), 0.01);
      assertEquals(0, snapshot.minLatencyMs());
      assertEquals(0, snapshot.maxLatencyMs());
    }

    @Test
    void snapshotTakenBeforeResetIsNotAffectedByReset() {
      collector.record(200, 100);
      collector.record(500, 200);

      StatsSnapshot before = collector.getSnapshot();
      collector.reset();

      assertEquals(2, before.totalRequests());
      assertEquals(1, before.successCount());
      assertEquals(1, before.errorCount());
      assertEquals(150, before.avgLatencyMs(), 0.01);
      assertEquals(100, before.minLatencyMs());
      assertEquals(200, before.maxLatencyMs());
    }
  }

  @Nested
  class ThreadSafety {

    @Test
    void concurrentWritesProduceCorrectTotalCount() throws InterruptedException {
      int threadCount = 10;
      int recordsPerThread = 1000;
      try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
          executor.submit(
              () -> {
                try {
                  for (int j = 0; j < recordsPerThread; j++) {
                    collector.record(200, 10);
                  }
                } finally {
                  latch.countDown();
                }
              });
        }

        latch.await();
      }

      StatsSnapshot snapshot = collector.getSnapshot();
      assertEquals(threadCount * recordsPerThread, snapshot.totalRequests());
      assertEquals(threadCount * recordsPerThread, snapshot.successCount());
      assertEquals(0, snapshot.errorCount());
    }

    @Test
    void tenCyclesRandomDataMultiThread() throws InterruptedException {
      var random = new java.util.Random(42);
      int threadCount = 5;

      for (int cycle = 0; cycle < 10; cycle++) {
        int recordsPerThread = random.nextInt(100) + 10;
        int totalRecords = threadCount * recordsPerThread;

        // pre-generate all data
        int[] statusCodes = new int[totalRecords];
        long[] latencies = new long[totalRecords];
        long expectedSuccess = 0;
        long expectedErrors = 0;
        long expectedTotalLatency = 0;
        long expectedMin = Long.MAX_VALUE;
        long expectedMax = 0;

        for (int i = 0; i < totalRecords; i++) {
          statusCodes[i] = random.nextInt(500) + 100;
          latencies[i] = random.nextInt(1000);

          if (200 <= statusCodes[i] && statusCodes[i] < 300) {
            expectedSuccess++;
          } else {
            expectedErrors++;
          }
          expectedTotalLatency += latencies[i];
          expectedMin = Math.min(expectedMin, latencies[i]);
          expectedMax = Math.max(expectedMax, latencies[i]);
        }

        double expectedAvg = (double) expectedTotalLatency / totalRecords;

        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
          CountDownLatch latch = new CountDownLatch(threadCount);

          for (int t = 0; t < threadCount; t++) {
            int from = t * recordsPerThread;
            int to = from + recordsPerThread;
            executor.submit(
                () -> {
                  try {
                    for (int j = from; j < to; j++) {
                      collector.record(statusCodes[j], latencies[j]);
                    }
                  } finally {
                    latch.countDown();
                  }
                });
          }

          latch.await();
        }

        StatsSnapshot snapshot = collector.getSnapshotAndReset();
        assertEquals(totalRecords, snapshot.totalRequests(), "cycle " + cycle + ": total");
        assertEquals(expectedSuccess, snapshot.successCount(), "cycle " + cycle + ": success");
        assertEquals(expectedErrors, snapshot.errorCount(), "cycle " + cycle + ": errors");
        assertEquals(expectedAvg, snapshot.avgLatencyMs(), 0.01, "cycle " + cycle + ": avg");
        assertEquals(expectedMin, snapshot.minLatencyMs(), "cycle " + cycle + ": min");
        assertEquals(expectedMax, snapshot.maxLatencyMs(), "cycle " + cycle + ": max");
      }
    }
  }
}
