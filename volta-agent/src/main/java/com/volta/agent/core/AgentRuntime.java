package com.volta.agent.core;

import com.volta.agent.engine.LoadEngine;
import com.volta.stats.StatsCollector;
import com.volta.stats.StatsSnapshot;

public class AgentRuntime {
  private volatile boolean isRunning = false;
  private LoadEngine currentEngine = null;
  private StatsCollector collector = new StatsCollector();

  public synchronized boolean start(String url, int targetRps, int durationSeconds) {
    if (isRunning) {
      return false;
    }

    collector = new StatsCollector();

    isRunning = true;
    currentEngine = new LoadEngine(url, targetRps, durationSeconds);

    Thread.ofPlatform()
        .start(
            () -> {
              currentEngine.start();
              isRunning = false;
            });

    return true;
  }

  public void stop() {
    if (currentEngine != null) {
      currentEngine.stop();
    }
  }

  public StatsSnapshot getStatsSnapshot() {
    return collector.getSnapshot();
  }

  public boolean isRunning() {
    return isRunning;
  }
}
