package com.volta.master.cluster;

import com.volta.master.client.AgentClient;
import com.volta.model.TestConfig;
import com.volta.stats.StatsAggregator;
import com.volta.stats.StatsSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AgentCluster {
  private static final Logger log = LoggerFactory.getLogger(AgentCluster.class);

  private final TestConfig testConfig;
  private final List<String> allAgentUrls;
  private final Set<String> deadAgentUrls = new LinkedHashSet<>();
  private final Map<String, Integer> assignedRps = new HashMap<>();

  public AgentCluster(TestConfig testConfig, List<String> agentUrls) {
    this.testConfig = testConfig;
    this.allAgentUrls = List.copyOf(agentUrls);
  }

  public static AgentCluster of(TestConfig testConfig, List<String> agentUrls) {
    return new AgentCluster(testConfig, agentUrls);
  }

  public List<String> allAgentUrls() {
    return allAgentUrls;
  }

  public List<String> aliveAgentUrls() {
    return allAgentUrls.stream().filter(url -> !deadAgentUrls.contains(url)).toList();
  }

  public List<String> deadAgentUrls() {
    return List.copyOf(deadAgentUrls);
  }

  public boolean hasDeadAgents() {
    return !deadAgentUrls.isEmpty();
  }

  public void startCluster(AgentClient agentClient) {
    List<String> reachable = new ArrayList<>();
    for (String agentUrl : allAgentUrls) {
      if (agentClient.isReachable(agentUrl)) {
        reachable.add(agentUrl);
      } else {
        markDead(agentUrl, "unreachable at startup");
      }
    }

    if (reachable.isEmpty()) {
      throw new IllegalStateException("No reachable agents");
    }

    startAgents(agentClient, reachable);
  }

  public StatsSnapshot fetchStatsWithFailover(AgentClient agentClient) {
    List<StatsSnapshot> snapshots = new ArrayList<>();
    Set<String> newlyDead = new HashSet<>();

    for (String agentUrl : aliveAgentUrls()) {
      try {
        snapshots.add(agentClient.getStats(agentUrl));
      } catch (Exception e) {
        newlyDead.add(agentUrl);
        log.error("Failed to fetch stats from {}: {}", agentUrl, e.getMessage());
      }
    }

    if (!newlyDead.isEmpty()) {
      for (String agentUrl : newlyDead) {
        markDead(agentUrl, "failed during test");
      }
      redistributeLoad(agentClient);
    }

    return StatsAggregator.merge(snapshots);
  }

  private void startAgents(AgentClient agentClient, List<String> aliveAgents) {
    List<Integer> rpsValues = LoadPlanner.splitRps(testConfig.rps(), aliveAgents.size());
    assignedRps.clear();

    for (int i = 0; i < aliveAgents.size(); i++) {
      String agentUrl = aliveAgents.get(i);
      int agentRps = rpsValues.get(i);
      TestConfig agentConfig =
          new TestConfig(agentRps, testConfig.duration(), testConfig.request());

      if (agentClient.startTestSafe(agentUrl, agentConfig)) {
        assignedRps.put(agentUrl, agentRps);
        log.info("Started agent {} with rps={}", agentUrl, agentRps);
      } else {
        markDead(agentUrl, "failed to start test");
      }
    }

    if (assignedRps.isEmpty()) {
      throw new IllegalStateException("Failed to start test on any agent");
    }

    if (assignedRps.size() < aliveAgents.size()) {
      redistributeLoad(agentClient);
    }
  }

  private void redistributeLoad(AgentClient agentClient) {
    List<String> aliveAgents = aliveAgentUrls().stream().filter(assignedRps::containsKey).toList();
    if (aliveAgents.isEmpty()) {
      log.error("No alive agents left for load redistribution");
      return;
    }

    List<Integer> rpsValues = LoadPlanner.splitRps(testConfig.rps(), aliveAgents.size());
    for (int i = 0; i < aliveAgents.size(); i++) {
      String agentUrl = aliveAgents.get(i);
      int newRps = rpsValues.get(i);
      int oldRps = assignedRps.getOrDefault(agentUrl, 0);

      if (oldRps == newRps) {
        continue;
      }

      if (agentClient.changeRps(agentUrl, newRps)) {
        assignedRps.put(agentUrl, newRps);
        log.warn("Redistributed load for {}: {} -> {} rps", agentUrl, oldRps, newRps);
        System.out.printf(
            "WARNING: redistributed load for %s: %d -> %d rps%n", agentUrl, oldRps, newRps);
      } else {
        markDead(agentUrl, "failed to change rps during failover");
      }
    }
  }

  private void markDead(String agentUrl, String reason) {
    if (deadAgentUrls.add(agentUrl)) {
      assignedRps.remove(agentUrl);
      log.warn("Agent marked dead ({}): {}", reason, agentUrl);
      System.out.printf("WARNING: agent unavailable (%s): %s%n", reason, agentUrl);
    }
  }
}
