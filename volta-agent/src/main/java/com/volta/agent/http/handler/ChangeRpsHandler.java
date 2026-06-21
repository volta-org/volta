package com.volta.agent.http.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.volta.agent.core.AgentRuntime;
import com.volta.model.RpsChange;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ChangeRpsHandler implements HttpHandler {
  private final AgentRuntime runtime;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public ChangeRpsHandler(AgentRuntime runtime) {
    this.runtime = runtime;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    if (!exchange.getRequestMethod().equals("POST")) {
      exchange.sendResponseHeaders(405, -1);
      return;
    }

    String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

    RpsChange change;
    try {
      change = objectMapper.readValue(json, RpsChange.class);
    } catch (Exception e) {
      String error = "Invalid JSON";
      byte[] errorBytes = error.getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(400, errorBytes.length);
      exchange.getResponseBody().write(errorBytes);
      exchange.close();
      return;
    }

    boolean updated;
    try {
      updated = runtime.changeRps(change.rps());
    } catch (IllegalArgumentException e) {
      String error = "Invalid parameters: " + e.getMessage();
      byte[] errorBytes = error.getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(400, errorBytes.length);
      exchange.getResponseBody().write(errorBytes);
      exchange.close();
      return;
    }

    if (updated) {
      String response = "RPS updated";
      byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(200, responseBytes.length);
      exchange.getResponseBody().write(responseBytes);
    } else {
      exchange.sendResponseHeaders(409, -1);
    }

    exchange.close();
  }
}
