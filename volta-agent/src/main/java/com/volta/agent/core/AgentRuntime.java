package com.volta.agent.core;

import com.volta.agent.engine.LoadEngine;
import com.volta.stats.StatsSnapshot;

public class AgentRuntime {
  private volatile boolean isRunning = false;
  private LoadEngine currentEngine = null;

  public synchronized boolean start(String url, int targetRps, int durationSeconds) {
    if (isRunning) {
      return false;
    }

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
    if (currentEngine == null) {
      return new StatsSnapshot(0, 0, 0, 0.0, 0, 0);
    }
    return currentEngine.getStats();
  }

  public boolean isRunning() {
    return isRunning;
  }
}
