package com.volta.agent.http;

import com.sun.net.httpserver.HttpServer;
import com.volta.agent.core.AgentRuntime;
import com.volta.agent.http.handler.StartHandler;
import com.volta.agent.http.handler.StatsHandler;
import com.volta.agent.http.handler.StopHandler;
import java.io.IOException;
import java.net.InetSocketAddress;

public class AgentHttpServer {
  private final HttpServer server;

  public AgentHttpServer(AgentRuntime runtime, int port) {
    try {
      this.server = HttpServer.create(new InetSocketAddress(port), 0);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    server.createContext("/start", new StartHandler(runtime));
    server.createContext("/stop", new StopHandler(runtime));
    server.createContext("/stats", new StatsHandler(runtime));
  }

  public void start() {
    server.start();
  }
}
