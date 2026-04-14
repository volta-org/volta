package com.volta.agent.engine;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.volta.agent.http.HttpSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoadEngineTest {

  private WireMockServer wireMock;
  private String baseUrl;
  private HttpSender sender;

  @BeforeEach
  void setUp() {
    wireMock = new WireMockServer(wireMockConfig().dynamicPort());
    wireMock.start();
    wireMock.stubFor(get("/test").willReturn(ok("OK")));
    baseUrl = "http://localhost:" + wireMock.port();

    try (HttpSender sender = new HttpSender()) {
      sender.send(baseUrl + "/test");
    } catch (Exception ignored) {
    }
  }

  @AfterEach
  void tearDown() throws Exception {
    if (sender != null) {
      sender.close();
    }
    wireMock.stop();
  }

  @Test
  void shouldSendRequestsAtTargetRps() {
    int targetRps = 10;
    int durationSeconds = 5;
    int expectedRequests = targetRps * durationSeconds;

    LoadEngine engine = new LoadEngine(baseUrl + "/test", targetRps, durationSeconds);
    engine.start();

    int actualRequests =
        wireMock.countRequestsMatching(getRequestedFor(urlEqualTo("/test")).build()).getCount();

    int minExpected = (int) (expectedRequests * 0.8);
    int maxExpected = (int) (expectedRequests * 1.2);

    assertTrue(
        actualRequests >= minExpected && actualRequests <= maxExpected,
        String.format(
            "Expected %d requests (±20%%), but got %d", expectedRequests, actualRequests));
  }

  @Test
  void shouldStopAutomaticallyAfterDuration() {
    int durationSeconds = 2;
    LoadEngine engine = new LoadEngine(baseUrl + "/test", 10, durationSeconds);

    long startTime = System.currentTimeMillis();
    engine.start();
    long elapsed = System.currentTimeMillis() - startTime;

    assertTrue(
        elapsed >= 2000 && elapsed < 3000,
        String.format("Expected ~2000ms, but took %dms", elapsed));
  }

  @Test
  void shouldStopManually() throws InterruptedException {
    int durationSeconds = 60;
    LoadEngine engine = new LoadEngine(baseUrl + "/test", 10, durationSeconds);

    Thread engineThread = new Thread(engine::start);
    engineThread.start();

    Thread.sleep(1000);
    engine.stop();
    engineThread.join(2000);

    assertFalse(engineThread.isAlive(), "Engine should have stopped");
  }

  @Test
  void shouldHandleServerErrors() {
    wireMock.stubFor(get("/error").willReturn(serverError()));
    LoadEngine engine = new LoadEngine(baseUrl + "/error", 10, 2);

    assertDoesNotThrow(engine::start);
  }

  @Test
  void shouldHandleInvalidUrl() {
    LoadEngine engine = new LoadEngine("http://invalid-host-that-does-not-exist:9999/test", 5, 2);

    assertDoesNotThrow(engine::start);
  }

  @Test
  void shouldRejectNullUrl() {
    assertThrows(IllegalArgumentException.class, () -> new LoadEngine(null, 10, 5));
  }

  @Test
  void shouldRejectEmptyUrl() {
    assertThrows(IllegalArgumentException.class, () -> new LoadEngine("", 10, 5));
  }

  @Test
  void shouldRejectZeroRps() {
    assertThrows(
        IllegalArgumentException.class, () -> new LoadEngine("http://localhost/test", 0, 5));
  }

  @Test
  void shouldRejectNegativeRps() {
    assertThrows(
        IllegalArgumentException.class, () -> new LoadEngine("http://localhost/test", -1, 5));
  }

  @Test
  void shouldRejectZeroDuration() {
    assertThrows(
        IllegalArgumentException.class, () -> new LoadEngine("http://localhost/test", 10, 0));
  }

  @Test
  void shouldRejectNegativeDuration() {
    assertThrows(
        IllegalArgumentException.class, () -> new LoadEngine("http://localhost/test", 10, -1));
  }
}
