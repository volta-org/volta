package com.volta.agent.http;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.volta.agent.core.AgentRuntime;
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
    assertTrue(response.body().contains("running"));
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
    assertTrue(beforeStart.body().contains("running: false")); // было "running":false

    postJson(agentBaseUrl + "/start", payload);
    Thread.sleep(100);

    HttpResponse<String> afterStart = sendGet(agentBaseUrl + "/stats");
    System.out.println("After: " + afterStart.body());
    assertTrue(afterStart.body().contains("running: true"));  // было "running":true
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