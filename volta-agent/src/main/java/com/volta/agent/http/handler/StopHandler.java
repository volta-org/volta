package com.volta.agent.http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.volta.agent.core.AgentRuntime;
import java.io.IOException;

public class StopHandler implements HttpHandler {
  private final AgentRuntime runtime;

  public StopHandler(AgentRuntime runtime) {
    this.runtime = runtime;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    if (!exchange.getRequestMethod().equals("POST")) {
      exchange.sendResponseHeaders(405, -1);
      return;
    }

    runtime.stop();

    String response = "Test stopped";
    exchange.sendResponseHeaders(200, response.length());
    exchange.getResponseBody().write(response.getBytes());
    exchange.close();
  }
}
