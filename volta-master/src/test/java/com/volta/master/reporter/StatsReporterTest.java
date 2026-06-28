package com.volta.master.reporter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.volta.master.client.AgentClient;
import com.volta.master.cluster.AgentCluster;
import com.volta.model.TestConfig;
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

  private AgentCluster singleAgentCluster() {
    return AgentCluster.of(
        TestConfig.ofGet("https://example.com", 10, 30), List.of("http://localhost:7070"));
  }

  private AgentCluster twoAgentCluster() {
    return AgentCluster.of(
        TestConfig.ofGet("https://example.com", 100, 30),
        List.of("http://localhost:7070", "http://localhost:7071"));
  }

  private void mockStartedSingleAgent() {
    when(agentClient.isReachable("http://localhost:7070")).thenReturn(true);
    when(agentClient.startTestSafe(eq("http://localhost:7070"), any(TestConfig.class)))
        .thenReturn(true);
  }

  private void mockStartedTwoAgents() {
    when(agentClient.isReachable(anyString())).thenReturn(true);
    when(agentClient.startTestSafe(anyString(), any(TestConfig.class))).thenReturn(true);
  }

  @Test
  void shouldPollStatsEverySecond() {
    mockStartedSingleAgent();
    AgentCluster cluster = singleAgentCluster();
    cluster.startCluster(agentClient);

    StatsSnapshot mockStats = new StatsSnapshot(100, 95, 5, 50.0, 10, 200);
    when(agentClient.getStats("http://localhost:7070")).thenReturn(mockStats);

    statsReporter.startReporting(cluster, 3, Optional.empty());

    verify(agentClient, times(4)).getStats("http://localhost:7070");
  }

  @Test
  void shouldPrintLiveStatsInCorrectFormat() {
    mockStartedSingleAgent();
    AgentCluster cluster = singleAgentCluster();
    cluster.startCluster(agentClient);

    StatsSnapshot mockStats = new StatsSnapshot(100, 98, 2, 45.5, 10, 150);
    when(agentClient.getStats("http://localhost:7070")).thenReturn(mockStats);

    statsReporter.startReporting(cluster, 1, Optional.empty());

    String output = outputCapture.toString();
    assertTrue(output.contains("RPS:"));
    assertTrue(output.contains("Success:"));
    assertTrue(output.contains("Avg:"));
    assertTrue(output.contains("Errors: 2"));
  }

  @Test
  void shouldPrintFinalStatsAfterLoop() {
    mockStartedSingleAgent();
    AgentCluster cluster = singleAgentCluster();
    cluster.startCluster(agentClient);

    StatsSnapshot mockStats = new StatsSnapshot(100, 95, 5, 50.0, 10, 200);
    when(agentClient.getStats("http://localhost:7070")).thenReturn(mockStats);

    statsReporter.startReporting(cluster, 1, Optional.empty());

    String output = outputCapture.toString();
    assertTrue(output.contains("FINAL STATS"));
    assertTrue(output.contains("Total Requests:"));
    assertTrue(output.contains("Success Rate:"));
  }

  @Test
  void shouldCalculateSuccessRateCorrectly() {
    mockStartedSingleAgent();
    AgentCluster cluster = singleAgentCluster();
    cluster.startCluster(agentClient);

    StatsSnapshot stats = new StatsSnapshot(100, 95, 5, 50.0, 10, 200);
    when(agentClient.getStats("http://localhost:7070")).thenReturn(stats);

    statsReporter.startReporting(cluster, 1, Optional.empty());

    String output = outputCapture.toString();
    assertTrue(output.contains("95") || output.contains("95.0"));
  }

  @Test
  void shouldHandleZeroRequests() {
    mockStartedSingleAgent();
    AgentCluster cluster = singleAgentCluster();
    cluster.startCluster(agentClient);

    StatsSnapshot emptyStats = new StatsSnapshot(0, 0, 0, 0.0, 0, 0);
    when(agentClient.getStats("http://localhost:7070")).thenReturn(emptyStats);

    statsReporter.startReporting(cluster, 1, Optional.empty());

    String output = outputCapture.toString();
    assertTrue(output.contains("RPS: 0"));
  }

  @Test
  void shouldContinueOnStatsError() {
    mockStartedSingleAgent();
    AgentCluster cluster = singleAgentCluster();
    cluster.startCluster(agentClient);

    when(agentClient.getStats("http://localhost:7070"))
        .thenThrow(new RuntimeException("Network error"))
        .thenReturn(new StatsSnapshot(10, 10, 0, 50.0, 10, 100));

    assertDoesNotThrow(() -> statsReporter.startReporting(cluster, 2, Optional.empty()));
    verify(agentClient, atLeast(1)).getStats("http://localhost:7070");
    assertTrue(outputCapture.toString().contains("WARNING: agent unavailable"));
  }

  @Test
  void shouldWriteCsvWithHeaderSampleAndFinalRows(@TempDir Path tempDir) throws Exception {
    mockStartedSingleAgent();
    AgentCluster cluster = singleAgentCluster();
    cluster.startCluster(agentClient);

    Path out = tempDir.resolve("report.csv");
    StatsSnapshot mockStats = new StatsSnapshot(100, 95, 5, 50.0, 10, 200);
    when(agentClient.getStats("http://localhost:7070")).thenReturn(mockStats);

    statsReporter.startReporting(cluster, 2, Optional.of(out.toString()));

    List<String> lines = Files.readAllLines(out);
    assertEquals(4, lines.size(), "header + 2 sample rows + final");
    assertTrue(lines.get(0).startsWith("kind,elapsedSeconds,"));
    assertTrue(lines.get(1).startsWith("sample,"));
    assertTrue(lines.get(2).startsWith("sample,"));
    assertTrue(lines.get(3).startsWith("final,"));
  }

  @Test
  void shouldAggregateStatsFromMultipleAgents() {
    mockStartedTwoAgents();
    AgentCluster cluster = twoAgentCluster();
    cluster.startCluster(agentClient);

    StatsSnapshot firstAgent = new StatsSnapshot(50, 45, 5, 100.0, 10, 200);
    StatsSnapshot secondAgent = new StatsSnapshot(50, 50, 0, 50.0, 20, 150);
    when(agentClient.getStats("http://localhost:7070")).thenReturn(firstAgent);
    when(agentClient.getStats("http://localhost:7071")).thenReturn(secondAgent);

    statsReporter.startReporting(cluster, 1, Optional.empty());

    String output = outputCapture.toString();
    assertTrue(output.contains("Total Requests:"));
    assertTrue(output.contains("100"));
    verify(agentClient, times(2)).getStats("http://localhost:7070");
    verify(agentClient, times(2)).getStats("http://localhost:7071");
  }

  @Test
  void shouldStopAfterSpecifiedDuration() {
    mockStartedSingleAgent();
    AgentCluster cluster = singleAgentCluster();
    cluster.startCluster(agentClient);

    StatsSnapshot mockStats = new StatsSnapshot(100, 100, 0, 50.0, 10, 100);
    when(agentClient.getStats("http://localhost:7070")).thenReturn(mockStats);

    long startTime = System.currentTimeMillis();
    statsReporter.startReporting(cluster, 2, Optional.empty());
    long elapsed = System.currentTimeMillis() - startTime;

    assertTrue(elapsed >= 2000 && elapsed < 3000);
  }
}
