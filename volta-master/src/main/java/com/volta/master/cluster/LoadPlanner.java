package com.volta.master.cluster;

import com.volta.model.TestConfig;
import java.util.ArrayList;
import java.util.List;

public final class LoadPlanner {

  private LoadPlanner() {}

  public static List<TestConfig> splitLoad(TestConfig config, int agentCount) {
    if (agentCount <= 0) {
      throw new IllegalArgumentException("agentCount must be positive");
    }

    List<Integer> rpsValues = splitRps(config.rps(), agentCount);
    List<TestConfig> configs = new ArrayList<>(agentCount);
    for (int rps : rpsValues) {
      configs.add(new TestConfig(rps, config.duration(), config.request()));
    }
    return configs;
  }

  public static List<Integer> splitRps(int totalRps, int agentCount) {
    if (agentCount <= 0) {
      throw new IllegalArgumentException("agentCount must be positive");
    }

    int baseRps = totalRps / agentCount;
    int remainder = totalRps % agentCount;

    List<Integer> rpsValues = new ArrayList<>(agentCount);
    for (int i = 0; i < agentCount; i++) {
      rpsValues.add(baseRps + (i < remainder ? 1 : 0));
    }
    return rpsValues;
  }
}
