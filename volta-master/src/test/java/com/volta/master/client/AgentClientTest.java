package com.volta.master.client;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.volta.model.TestConfig;
import com.volta.stats.StatsSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class AgentClientTest {

  private WireMockServer wireMock;
  private AgentClient agentClient;
  private String agentUrl;

  @BeforeEach
  void setUp() {
    wireMock = new WireMockServer(wireMockConfig().dynamicPort());
    wireMock.start();
    agentUrl = "http://localhost:" + wireMock.port();

    agentClient = new AgentClient(RestClient.builder());
  }

  @AfterEach
  void tearDown() {
    wireMock.stop();
  }

  @Test
  void startTestSendsPostRequest() {
    wireMock.stubFor(post("/start").willReturn(ok()));

    TestConfig config = new TestConfig("http://example.com", 10, 5);
    agentClient.startTest(agentUrl, config);

    wireMock.verify(postRequestedFor(urlEqualTo("/start")));
  }

  @Test
  void stopTestSendsPostRequest() {
    wireMock.stubFor(post("/stop").willReturn(ok()));

    agentClient.stopTest(agentUrl);

    wireMock.verify(postRequestedFor(urlEqualTo("/stop")));
  }

  @Test
  void getStatsReturnsParsedResponse() {
    String statsJson =
        """
            {
              "totalRequests": 100,
              "successCount": 95,
              "errorCount": 5,
              "avgLatencyMs": 50.5,
              "minLatencyMs": 10,
              "maxLatencyMs": 200
            }
            """;

    wireMock.stubFor(
        get("/stats").willReturn(ok(statsJson).withHeader("Content-Type", "application/json")));

    StatsSnapshot stats = agentClient.getStats(agentUrl);

    assertEquals(100, stats.totalRequests());
    assertEquals(95, stats.successCount());
    assertEquals(5, stats.errorCount());
  }

  @Test
  void startTestSendsJsonBody() {
    wireMock.stubFor(post("/start").withRequestBody(matchingJsonPath("$.url")).willReturn(ok()));

    TestConfig config = new TestConfig("http://example.com", 10, 5);
    agentClient.startTest(agentUrl, config);

    wireMock.verify(postRequestedFor(urlEqualTo("/start")));
  }
}
