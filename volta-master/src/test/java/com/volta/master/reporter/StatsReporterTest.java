package com.volta.master.reporter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.volta.master.client.AgentClient;
import com.volta.stats.StatsSnapshot;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class StatsReporterTest {

  @Mock private AgentClient agentClient;

  private StatsReporter statsReporter;
  private ByteArrayOutputStream outputCapture;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    statsReporter = new StatsReporter(agentClient);
    outputCapture = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outputCapture));
  }

  @Test
  void shouldPollStatsEverySecond() {
    // Verify that StatsReporter calls getStats() once per second plus once for final stats
    StatsSnapshot mockStats = new StatsSnapshot(100, 95, 5, 50.0, 10, 200);
    when(agentClient.getStats(anyString())).thenReturn(mockStats);

    statsReporter.startReporting("http://localhost:7070", 3, Optional.empty());

    verify(agentClient, times(4)).getStats("http://localhost:7070");
  }

  @Test
  void shouldPrintLiveStatsInCorrectFormat() {
    // Verify that live stats are printed in format [RPS: X | Success: Y% | Avg: Zms | Errors: N]
    StatsSnapshot mockStats = new StatsSnapshot(100, 98, 2, 45.5, 10, 150);
    when(agentClient.getStats(anyString())).thenReturn(mockStats);

    statsReporter.startReporting("http://localhost:7070", 1, Optional.empty());

    String output = outputCapture.toString();
    assertTrue(output.contains("RPS:"));
    assertTrue(output.contains("Success:"));
    assertTrue(output.contains("Avg:"));
    assertTrue(output.contains("Errors: 2"));
  }

  @Test
  void shouldPrintFinalStatsAfterLoop() {
    // Verify that final stats summary is printed after reporting loop completes
    StatsSnapshot mockStats = new StatsSnapshot(100, 95, 5, 50.0, 10, 200);
    when(agentClient.getStats(anyString())).thenReturn(mockStats);

    statsReporter.startReporting("http://localhost:7070", 1, Optional.empty());

    String output = outputCapture.toString();
    assertTrue(output.contains("FINAL STATS"));
    assertTrue(output.contains("Total Requests:"));
    assertTrue(output.contains("Success Rate:"));
  }

  @Test
  void shouldCalculateSuccessRateCorrectly() {
    // Verify that success rate is calculated correctly: (successCount / totalRequests) * 100
    StatsSnapshot stats = new StatsSnapshot(100, 95, 5, 50.0, 10, 200);
    when(agentClient.getStats(anyString())).thenReturn(stats);

    statsReporter.startReporting("http://localhost:7070", 1, Optional.empty());

    String output = outputCapture.toString();
    assertTrue(output.contains("95") || output.contains("95.0"));
  }

  @Test
  void shouldHandleZeroRequests() {
    // Verify that division by zero is handled gracefully when no requests have been sent
    StatsSnapshot emptyStats = new StatsSnapshot(0, 0, 0, 0.0, 0, 0);
    when(agentClient.getStats(anyString())).thenReturn(emptyStats);

    statsReporter.startReporting("http://localhost:7070", 1, Optional.empty());

    String output = outputCapture.toString();
    assertTrue(output.contains("RPS: 0"));
  }

  @Test
  void shouldContinueOnStatsError() {
    // Verify that StatsReporter continues operating even if getStats() throws an exception
    when(agentClient.getStats(anyString()))
        .thenThrow(new RuntimeException("Network error"))
        .thenReturn(new StatsSnapshot(10, 10, 0, 50.0, 10, 100));

    assertDoesNotThrow(
        () -> statsReporter.startReporting("http://localhost:7070", 2, Optional.empty()));
    verify(agentClient, atLeast(2)).getStats(anyString());
  }

  @Test
  void shouldWriteCsvWithHeaderSampleAndFinalRows(@TempDir Path tempDir) throws Exception {
    Path out = tempDir.resolve("report.csv");
    StatsSnapshot mockStats = new StatsSnapshot(100, 95, 5, 50.0, 10, 200);
    when(agentClient.getStats(anyString())).thenReturn(mockStats);

    statsReporter.startReporting("http://localhost:7070", 2, Optional.of(out.toString()));

    List<String> lines = Files.readAllLines(out);
    assertEquals(4, lines.size(), "header + 2 sample rows + final");
    assertTrue(lines.get(0).startsWith("kind,elapsedSeconds,"));
    assertTrue(lines.get(1).startsWith("sample,"));
    assertTrue(lines.get(2).startsWith("sample,"));
    assertTrue(lines.get(3).startsWith("final,"));
  }

  @Test
  void shouldStopAfterSpecifiedDuration() {
    // Verify that reporting loop stops after the specified duration (±500ms tolerance)
    StatsSnapshot mockStats = new StatsSnapshot(100, 100, 0, 50.0, 10, 100);
    when(agentClient.getStats(anyString())).thenReturn(mockStats);

    long startTime = System.currentTimeMillis();
    statsReporter.startReporting("http://localhost:7070", 2, Optional.empty());
    long elapsed = System.currentTimeMillis() - startTime;

    assertTrue(elapsed >= 2000 && elapsed < 3000);
  }
}
