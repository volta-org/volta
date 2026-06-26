package com.volta.master.client;

import com.volta.model.RpsChange;
import com.volta.model.TestConfig;
import com.volta.stats.StatsSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AgentClient {
  private static final Logger log = LoggerFactory.getLogger(AgentClient.class);

  private final RestClient restClient;

  public AgentClient(RestClient.Builder builder) {
    this.restClient = builder.build();
  }

  public void startTest(String agentUrl, TestConfig config) {
    restClient.post().uri(agentUrl + "/start").body(config).retrieve().toBodilessEntity();
  }

  public boolean startTestSafe(String agentUrl, TestConfig config) {
    try {
      startTest(agentUrl, config);
      return true;
    } catch (Exception e) {
      log.error("Failed to start test on {}: {}", agentUrl, e.getMessage());
      return false;
    }
  }

  public void stopTest(String agentUrl) {
    restClient.post().uri(agentUrl + "/stop").retrieve().toBodilessEntity();
  }

  public boolean changeRps(String agentUrl, int rps) {
    try {
      restClient
          .post()
          .uri(agentUrl + "/change-rps")
          .body(new RpsChange(rps))
          .retrieve()
          .toBodilessEntity();
      return true;
    } catch (Exception e) {
      log.error("Failed to change rps on {}: {}", agentUrl, e.getMessage());
      return false;
    }
  }

  public StatsSnapshot getStats(String agentUrl) {
    return restClient.get().uri(agentUrl + "/stats").retrieve().body(StatsSnapshot.class);
  }

  public boolean isReachable(String agentUrl) {
    try {
      getStats(agentUrl);
      return true;
    } catch (Exception e) {
      log.error("Agent unreachable {}: {}", agentUrl, e.getMessage());
      return false;
    }
  }
}
