package com.volta.agent.core;

import com.volta.agent.engine.LoadEngine;
import com.volta.model.TestConfig;
import com.volta.stats.StatsSnapshot;

public class AgentRuntime {
  private volatile boolean isRunning = false;
  private LoadEngine currentEngine = null;

  public synchronized boolean start(TestConfig config) {
    if (isRunning) {
      return false;
    }

    isRunning = true;
    currentEngine = new LoadEngine(config.request(), config.rps(), config.duration());

    Thread.ofPlatform()
        .start(
            () -> {
              currentEngine.start();
              isRunning = false;
            });

    return true;
  }

  public synchronized void stop() {
    if (currentEngine != null) {
      currentEngine.stop();
    }
    isRunning = false;
  }

  public synchronized StatsSnapshot getStatsSnapshot() {
    if (currentEngine == null) {
      return new StatsSnapshot(0, 0, 0, 0.0, 0, 0);
    }
    return currentEngine.getStats();
  }

  public synchronized StatsSnapshot getStatsSnapshotAndReset() {
    if (currentEngine == null) {
      return new StatsSnapshot(0, 0, 0, 0.0, 0, 0);
    }
    return currentEngine.getStatsAndReset();
  }

  public synchronized boolean isRunning() {
    return isRunning;
  }

  public synchronized boolean changeRps(int rps) {
    if (!isRunning || currentEngine == null) {
      return false;
    }
    currentEngine.updateTargetRps(rps);
    return true;
  }
}
