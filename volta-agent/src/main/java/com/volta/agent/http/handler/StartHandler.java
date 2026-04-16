package com.volta.agent.http.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.volta.agent.core.AgentRuntime;
import com.volta.model.TestConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class StartHandler implements HttpHandler {
  private final AgentRuntime runtime;
  private final ObjectMapper objectMapper = new ObjectMapper();

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
      config = objectMapper.readValue(json, TestConfig.class);
    } catch (Exception e) {
      String error = "Invalid JSON";
      byte[] errorBytes = error.getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(400, errorBytes.length);
      exchange.getResponseBody().write(errorBytes);
      exchange.close();
      return;
    }

    boolean started;
    try {
      started = runtime.start(config.url(), config.rps(), config.duration());
    } catch (IllegalArgumentException e) {
      String error = "Invalid parameters: " + e.getMessage();
      byte[] errorBytes = error.getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(400, errorBytes.length);
      exchange.getResponseBody().write(errorBytes);
      exchange.close();
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
}
