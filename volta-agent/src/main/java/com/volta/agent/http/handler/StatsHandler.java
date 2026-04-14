package com.volta.agent.http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.volta.agent.core.AgentRuntime;
import java.io.IOException;

public class StatsHandler implements HttpHandler {
  private final AgentRuntime runtime;

  public StatsHandler(AgentRuntime runtime) {
    this.runtime = runtime;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    if (!exchange.getRequestMethod().equals("GET")) {
      exchange.sendResponseHeaders(405, -1);
      return;
    }

    String response = "{\"status\": \"running: " + runtime.isRunning() + "\"}";

    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(200, response.length());
    exchange.getResponseBody().write(response.getBytes());
    exchange.close();
  }
}
