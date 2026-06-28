package com.volta.master.cluster;

import static org.junit.jupiter.api.Assertions.*;

import com.volta.model.TestConfig;
import java.util.List;
import org.junit.jupiter.api.Test;

class LoadPlannerTest {

  @Test
  void splitRpsDistributesRemainderToFirstAgents() {
    assertEquals(List.of(4, 3, 3), LoadPlanner.splitRps(10, 3));
    assertEquals(10, LoadPlanner.splitRps(10, 3).stream().mapToInt(Integer::intValue).sum());
  }

  @Test
  void splitLoadDistributesRemainderToFirstAgents() {
    TestConfig config = TestConfig.ofGet("https://example.com", 10, 30);

    List<TestConfig> split = LoadPlanner.splitLoad(config, 3);

    assertEquals(3, split.size());
    assertEquals(4, split.get(0).rps());
    assertEquals(3, split.get(1).rps());
    assertEquals(3, split.get(2).rps());
    assertEquals(10, split.stream().mapToInt(TestConfig::rps).sum());
    assertTrue(split.stream().allMatch(c -> c.request().equals(config.request())));
    assertTrue(split.stream().allMatch(c -> c.duration() == config.duration()));
  }

  @Test
  void splitLoadWithSingleAgentKeepsFullRps() {
    TestConfig config = TestConfig.ofGet("https://example.com", 25, 10);

    List<TestConfig> split = LoadPlanner.splitLoad(config, 1);

    assertEquals(1, split.size());
    assertEquals(25, split.get(0).rps());
  }

  @Test
  void splitLoadRejectsNonPositiveAgentCount() {
    TestConfig config = TestConfig.ofGet("https://example.com", 10, 10);

    assertThrows(IllegalArgumentException.class, () -> LoadPlanner.splitLoad(config, 0));
  }
}
