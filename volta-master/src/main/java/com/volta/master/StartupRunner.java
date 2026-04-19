package com.volta.master;

import com.volta.master.cli.ArgsException;
import com.volta.master.cli.ArgsParser;
import com.volta.master.cli.MasterArgs;
import com.volta.master.client.AgentClient;
import com.volta.master.reporter.StatsReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupRunner implements ApplicationRunner {

  private final AgentClient agentClient;
  private final StatsReporter statsReporter;

  private static final Logger log = LoggerFactory.getLogger(StartupRunner.class);

  public StartupRunner(AgentClient agentClient, StatsReporter statsReporter) {
    this.agentClient = agentClient;
    this.statsReporter = statsReporter;
  }

  @Override
  public void run(ApplicationArguments args) {
    MasterArgs masterArgs;
    try {
      masterArgs = ArgsParser.parse(args);
    } catch (ArgsException e) {
      log.error(e.getMessage());
      System.exit(1);
      return;
    }

    log.info(
        "Starting test: url={}, rps={}, duration={}s, agent={}",
        masterArgs.testConfig().url(),
        masterArgs.testConfig().rps(),
        masterArgs.testConfig().duration(),
        masterArgs.agentUrl());

    agentClient.startTest("http://" + masterArgs.agentUrl(), masterArgs.testConfig());

    statsReporter.startReporting(masterArgs.agentUrl(), masterArgs.testConfig().duration());
  }
}
