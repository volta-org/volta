package com.volta.agent;

import com.volta.agent.core.AgentRuntime;
import com.volta.agent.http.AgentHttpServer;

public class AgentMain {
  public static void main(String[] args) {
    int port = 7070;
    if (args.length > 0) {
      port = Integer.parseInt(args[0]);
    }

    AgentRuntime runtime = new AgentRuntime();
    AgentHttpServer server = new AgentHttpServer(runtime, port);
    server.start();
    System.out.println("Agent started on port " + port);
  }
}
