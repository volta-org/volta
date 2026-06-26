package com.volta.master;

import com.volta.master.cli.ArgsException;
import com.volta.master.cli.ArgsParser;
import com.volta.master.cli.MasterArgs;
import com.volta.master.client.AgentClient;
import com.volta.master.cluster.AgentCluster;
import com.volta.master.reporter.StatsReporter;
import java.util.List;
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

    List<String> agentUrls = masterArgs.agentUrls();
    AgentCluster cluster = AgentCluster.of(masterArgs.testConfig(), agentUrls);

    log.info(
        "Starting test: url={}, totalRps={}, duration={}s, agents={}",
        masterArgs.testConfig().url(),
        masterArgs.testConfig().rps(),
        masterArgs.testConfig().duration(),
        agentUrls);

    try {
      cluster.startCluster(agentClient);
    } catch (IllegalStateException e) {
      log.error(e.getMessage());
      System.exit(1);
      return;
    }

    statsReporter.startReporting(
        cluster, masterArgs.testConfig().duration(), masterArgs.outputFile());
  }
}
