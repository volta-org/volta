package com.volta.agent.http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.volta.agent.core.AgentRuntime;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

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

    if (!runtime.isRunning()) {
      exchange.sendResponseHeaders(409, -1);
      return;
    }

    runtime.stop();

    String response = "Test stopped";
    byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
    exchange.sendResponseHeaders(200, responseBytes.length);
    exchange.getResponseBody().write(responseBytes);
    exchange.close();
  }
}
