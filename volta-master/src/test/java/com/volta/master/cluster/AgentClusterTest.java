package com.volta.master.cluster;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.volta.master.client.AgentClient;
import com.volta.model.TestConfig;
import com.volta.stats.StatsSnapshot;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class AgentClusterTest {

  @Mock private AgentClient agentClient;

  private TestConfig testConfig;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    testConfig = new TestConfig("https://example.com", 100, 30);
  }

  @Test
  void startClusterSkipsUnreachableAgentsAndRedistributesLoad() {
    when(agentClient.isReachable("http://localhost:7070")).thenReturn(true);
    when(agentClient.isReachable("http://localhost:7071")).thenReturn(false);
    when(agentClient.isReachable("http://localhost:7072")).thenReturn(true);
    when(agentClient.startTestSafe(eq("http://localhost:7070"), any(TestConfig.class)))
        .thenReturn(true);
    when(agentClient.startTestSafe(eq("http://localhost:7072"), any(TestConfig.class)))
        .thenReturn(true);

    AgentCluster cluster =
        AgentCluster.of(
            testConfig,
            List.of("http://localhost:7070", "http://localhost:7071", "http://localhost:7072"));

    cluster.startCluster(agentClient);

    assertEquals(List.of("http://localhost:7071"), cluster.deadAgentUrls());
    assertEquals(
        List.of("http://localhost:7070", "http://localhost:7072"), cluster.aliveAgentUrls());

    verify(agentClient)
        .startTestSafe("http://localhost:7070", new TestConfig("https://example.com", 50, 30));
    verify(agentClient)
        .startTestSafe("http://localhost:7072", new TestConfig("https://example.com", 50, 30));
    verify(agentClient, never()).startTestSafe(eq("http://localhost:7071"), any());
  }

  @Test
  void fetchStatsWithFailoverRedistributesLoadWhenAgentDies() {
    when(agentClient.isReachable(anyString())).thenReturn(true);
    when(agentClient.startTestSafe(anyString(), any(TestConfig.class))).thenReturn(true);
    when(agentClient.getStats("http://localhost:7070"))
        .thenReturn(new StatsSnapshot(10, 10, 0, 50.0, 10, 100));
    when(agentClient.getStats("http://localhost:7071"))
        .thenThrow(new RuntimeException("connection refused"))
        .thenReturn(new StatsSnapshot(0, 0, 0, 0.0, 0, 0));
    when(agentClient.changeRps("http://localhost:7070", 100)).thenReturn(true);

    AgentCluster cluster =
        AgentCluster.of(testConfig, List.of("http://localhost:7070", "http://localhost:7071"));
    cluster.startCluster(agentClient);

    StatsSnapshot stats = cluster.fetchStatsWithFailover(agentClient);

    assertEquals(10, stats.totalRequests());
    assertTrue(cluster.deadAgentUrls().contains("http://localhost:7071"));
    verify(agentClient).changeRps("http://localhost:7070", 100);
  }

  @Test
  void startClusterFailsWhenNoAgentsAreReachable() {
    when(agentClient.isReachable(anyString())).thenReturn(false);

    AgentCluster cluster =
        AgentCluster.of(testConfig, List.of("http://localhost:7070", "http://localhost:7071"));

    assertThrows(IllegalStateException.class, () -> cluster.startCluster(agentClient));
  }
}
