package com.volta.agent.http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.volta.agent.core.AgentRuntime;
import com.volta.model.TestConfig;
import com.volta.model.TestConfigReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class StartHandler implements HttpHandler {
  private final AgentRuntime runtime;

  public StartHandler(AgentRuntime runtime) {
    this.runtime = runtime;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    if (!exchange.getRequestMethod().equals("POST")) {
      exchange.sendResponseHeaders(405, -1);
      return;
    }

    String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

    TestConfig config;
    try {
      config = TestConfigReader.readJson(json);
    } catch (IllegalArgumentException e) {
      sendError(exchange, 400, e.getMessage());
      return;
    } catch (Exception e) {
      sendError(exchange, 400, "Invalid JSON");
      return;
    }

    boolean started;
    try {
      started = runtime.start(config);
    } catch (IllegalArgumentException e) {
      sendError(exchange, 400, "Invalid parameters: " + e.getMessage());
      return;
    }

    if (started) {
      String response = "Test started";
      byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
      exchange.sendResponseHeaders(200, responseBytes.length);
      exchange.getResponseBody().write(responseBytes);
    } else {
      exchange.sendResponseHeaders(409, -1);
    }

    exchange.close();
  }

  private static void sendError(HttpExchange exchange, int status, String error)
      throws IOException {
    byte[] errorBytes = error.getBytes(StandardCharsets.UTF_8);
    exchange.sendResponseHeaders(status, errorBytes.length);
    exchange.getResponseBody().write(errorBytes);
    exchange.close();
  }
}
