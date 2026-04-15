package com.volta.agent.http.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.volta.agent.core.AgentRuntime;
import com.volta.stats.StatsSnapshot;
import java.io.IOException;

public class StatsHandler implements HttpHandler {
  private final AgentRuntime runtime;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public StatsHandler(AgentRuntime runtime) {
    this.runtime = runtime;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    if (!exchange.getRequestMethod().equals("GET")) {
      exchange.sendResponseHeaders(405, -1);
      return;
    }

    StatsSnapshot stats = runtime.getStatsSnapshot();
    String json = objectMapper.writeValueAsString(stats);

    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(200, json.length());
    exchange.getResponseBody().write(json.getBytes());
    exchange.close();
  }
}
