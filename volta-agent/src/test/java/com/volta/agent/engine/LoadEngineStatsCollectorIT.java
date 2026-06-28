package com.volta.agent.engine;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.volta.model.RequestSpec;
import com.volta.stats.StatsSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LoadEngineStatsCollectorIT {
  private WireMockServer wireMock;
  private String baseUrl;

  @BeforeEach
  void setUp() {
    wireMock = new WireMockServer(wireMockConfig().dynamicPort());
    wireMock.start();
    baseUrl = "http://localhost:" + wireMock.port();
  }

  @AfterEach
  void tearDown() {
    wireMock.stop();
  }

  @Test
  void successfulRequestsAreRecordedInStats() {
    wireMock.stubFor(get("/test").willReturn(ok()));

    int rps = 5;
    int duration = 2;
    LoadEngine engine = new LoadEngine(RequestSpec.get(baseUrl + "/test"), rps, duration);
    engine.start();

    StatsSnapshot stats = engine.getStatsAndReset();
    int expected = rps * duration;

    assertTrue(
        stats.totalRequests() >= expected * 0.8,
        "Expected at least " + (int) (expected * 0.8) + " requests, got " + stats.totalRequests());
    assertEquals(stats.totalRequests(), stats.successCount(), "All requests should be successful");
    assertEquals(0, stats.errorCount());
    assertTrue(stats.avgLatencyMs() >= 0);
  }

  @Test
  void serverErrorsAreRecordedAsErrors() {
    wireMock.stubFor(get("/error").willReturn(serverError()));

    LoadEngine engine = new LoadEngine(RequestSpec.get(baseUrl + "/error"), 5, 2);
    engine.start();

    StatsSnapshot stats = engine.getStatsAndReset();
    assertTrue(stats.totalRequests() > 0);
    assertEquals(0, stats.successCount());
    assertEquals(stats.totalRequests(), stats.errorCount());
  }

  @Test
  void networkFailuresAreRecordedAsErrors() {
    LoadEngine engine =
        new LoadEngine(RequestSpec.get("http://invalid-host-that-does-not-exist:9999/test"), 5, 2);
    engine.start();

    StatsSnapshot stats = engine.getStatsAndReset();
    assertTrue(stats.totalRequests() > 0);
    assertEquals(0, stats.successCount());
    assertEquals(stats.totalRequests(), stats.errorCount());
  }

  @Test
  void mixedResponsesAreRecordedCorrectly() {
    wireMock.stubFor(
        get("/mixed")
            .inScenario("mixed")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(ok())
            .willSetStateTo("error"));
    wireMock.stubFor(
        get("/mixed")
            .inScenario("mixed")
            .whenScenarioStateIs("error")
            .willReturn(serverError())
            .willSetStateTo(Scenario.STARTED));

    LoadEngine engine = new LoadEngine(RequestSpec.get(baseUrl + "/mixed"), 10, 2);
    engine.start();

    StatsSnapshot stats = engine.getStatsAndReset();
    assertTrue(stats.successCount() > 0, "Should have some successes");
    assertTrue(stats.errorCount() > 0, "Should have some errors");
    assertEquals(
        stats.totalRequests(),
        stats.successCount() + stats.errorCount(),
        "total = success + errors");
  }
}
