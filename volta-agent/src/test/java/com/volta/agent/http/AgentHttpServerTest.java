package com.volta.agent.http;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.volta.agent.core.AgentRuntime;
import com.volta.stats.StatsSnapshot;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AgentHttpServerTest {

  private final HttpClient client = HttpClient.newHttpClient();

  private WireMockServer wireMock;
  private AgentHttpServer agentServer;
  private String agentBaseUrl;

  @BeforeEach
  void setUp() {
    wireMock = new WireMockServer(wireMockConfig().dynamicPort());
    wireMock.start();
    wireMock.stubFor(get("/test").willReturn(ok("OK")));

    int port = findFreePort();
    agentServer = new AgentHttpServer(new AgentRuntime(), port);
    agentServer.start();
    agentBaseUrl = "http://localhost:" + port;
  }

  @AfterEach
  void tearDown() {
    wireMock.stop();
  }

  @Test
  void startReturns200() throws Exception {
    String payload =
        """
            {"url":"http://localhost:%d/test","rps":1,"duration":2}
            """
            .formatted(wireMock.port());

    HttpResponse<String> response = postJson(agentBaseUrl + "/start", payload);

    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("Test started"));
  }

  @Test
  void startReturns409WhenAlreadyRunning() throws Exception {
    String payload =
        """
            {"url":"http://localhost:%d/test","rps":1,"duration":10}
            """
            .formatted(wireMock.port());

    HttpResponse<String> first = postJson(agentBaseUrl + "/start", payload);
    HttpResponse<String> second = postJson(agentBaseUrl + "/start", payload);

    assertEquals(200, first.statusCode());
    assertEquals(409, second.statusCode());
  }

  @Test
  void statsReturns200BeforeStart() throws Exception {
    HttpResponse<String> response = sendGet(agentBaseUrl + "/stats");

    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("totalRequests"));
  }

  @Test
  void stopReturns200() throws Exception {
    String payload =
        """
            {"url":"http://localhost:%d/test","rps":1,"duration":60}
            """
            .formatted(wireMock.port());

    postJson(agentBaseUrl + "/start", payload);
    HttpResponse<String> response = postJson(agentBaseUrl + "/stop", "");

    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("Test stopped"));
  }

  @Test
  void statsReturnsCorrectRunningState() throws Exception {
    String payload =
        """
            {"url":"http://localhost:%d/test","rps":1,"duration":10}
            """
            .formatted(wireMock.port());

    HttpResponse<String> beforeStart = sendGet(agentBaseUrl + "/stats");
    System.out.println("Before: " + beforeStart.body());
    assertTrue(beforeStart.body().contains("\"totalRequests\":0"));

    postJson(agentBaseUrl + "/start", payload);
    Thread.sleep(200);

    HttpResponse<String> afterStart = sendGet(agentBaseUrl + "/stats");
    System.out.println("After: " + afterStart.body());
    assertTrue(afterStart.body().contains("\"totalRequests\":"));
  }

  @Test
  void startReturns400ForInvalidJson() throws Exception {
    HttpResponse<String> response = postJson(agentBaseUrl + "/start", "{invalid json}");

    assertEquals(400, response.statusCode());
  }

  @Test
  void startReturns405ForGetRequest() throws Exception {
    HttpResponse<String> response = sendGet(agentBaseUrl + "/start");

    assertEquals(405, response.statusCode());
  }

  @Test
  void statsShowsRealRequestCount() throws Exception {
    String payload =
        """
            {"url":"http://localhost:%d/test","rps":5,"duration":2}
            """
            .formatted(wireMock.port());

    postJson(agentBaseUrl + "/start", payload);
    Thread.sleep(2500);

    HttpResponse<String> response = sendGet(agentBaseUrl + "/stats");

    assertTrue(response.body().contains("\"totalRequests\":"));
    StatsSnapshot stats = new ObjectMapper().readValue(response.body(), StatsSnapshot.class);
    assertTrue(
        stats.totalRequests() >= 8 && stats.totalRequests() <= 12,
        "Expected ~10 requests, got " + stats.totalRequests());
  }

  @Test
  void statsShowsSuccessCount() throws Exception {
    wireMock.stubFor(get("/success").willReturn(ok("OK")));

    String payload =
        """
            {"url":"http://localhost:%d/success","rps":5,"duration":1}
            """
            .formatted(wireMock.port());

    postJson(agentBaseUrl + "/start", payload);
    Thread.sleep(1500);

    HttpResponse<String> response = sendGet(agentBaseUrl + "/stats");
    StatsSnapshot stats = new ObjectMapper().readValue(response.body(), StatsSnapshot.class);

    assertTrue(stats.successCount() > 0, "Should have successful requests");
    assertEquals(0, stats.errorCount(), "Should have no errors");
  }

  @Test
  void statsShowsErrorCount() throws Exception {
    wireMock.stubFor(get("/error").willReturn(serverError()));

    String payload =
        """
            {"url":"http://localhost:%d/error","rps":5,"duration":1}
            """
            .formatted(wireMock.port());

    postJson(agentBaseUrl + "/start", payload);
    Thread.sleep(1500);

    HttpResponse<String> response = sendGet(agentBaseUrl + "/stats");
    StatsSnapshot stats = new ObjectMapper().readValue(response.body(), StatsSnapshot.class);

    assertTrue(stats.errorCount() > 0, "Should have errors");
    assertEquals(0, stats.successCount(), "Should have no successful requests");
  }

  @Test
  void statsShowsLatency() throws Exception {
    wireMock.stubFor(get("/slow").willReturn(ok().withFixedDelay(100)));

    String payload =
        """
            {"url":"http://localhost:%d/slow","rps":5,"duration":1}
            """
            .formatted(wireMock.port());

    postJson(agentBaseUrl + "/start", payload);
    Thread.sleep(1500);

    HttpResponse<String> response = sendGet(agentBaseUrl + "/stats");
    StatsSnapshot stats = new ObjectMapper().readValue(response.body(), StatsSnapshot.class);

    assertTrue(
        stats.avgLatencyMs() >= 90, "Avg latency should be ~100ms, got " + stats.avgLatencyMs());
    assertTrue(stats.minLatencyMs() >= 90, "Min latency should be ~100ms");
  }

  private HttpResponse<String> postJson(String url, String json) throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> sendGet(String url) throws Exception {
    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private int findFreePort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (IOException e) {
      throw new RuntimeException("Cannot allocate free port", e);
    }
  }
}
