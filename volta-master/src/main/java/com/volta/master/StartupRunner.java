package com.volta.master;

import com.volta.master.cli.ArgsException;
import com.volta.master.cli.ArgsParser;
import com.volta.model.TestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupRunner implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(StartupRunner.class);

  @Override
  public void run(ApplicationArguments args) {
    TestConfig config;
    try {
      config = ArgsParser.parse(args);
    } catch (ArgsException e) {
      log.error(e.getMessage());
      System.exit(1);
      return;
    }

    log.info(
        "Starting test: url={}, rps={}, duration={}s, agent={}",
        config.url(),
        config.rps(),
        config.durationSeconds(),
        config.agent());

    // TODO
    // agentClient.startTest(config);
  }
}
