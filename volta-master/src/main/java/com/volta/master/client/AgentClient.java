package com.volta.master.client;

import com.volta.model.TestConfig;
import com.volta.stats.StatsSnapshot;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AgentClient {
  private final RestClient restClient;

  public AgentClient(RestClient.Builder builder) {
    this.restClient = builder.build();
  }

  public void startTest(String agentUrl, TestConfig config) {
    restClient.post().uri(agentUrl + "/start").body(config).retrieve().toBodilessEntity();
  }

  public void stopTest(String agentUrl) {
    restClient.post().uri(agentUrl + "/stop").retrieve().toBodilessEntity();
  }

  public StatsSnapshot getStats(String agentUrl) {
    return restClient.get().uri(agentUrl + "/stats").retrieve().body(StatsSnapshot.class);
  }
}
