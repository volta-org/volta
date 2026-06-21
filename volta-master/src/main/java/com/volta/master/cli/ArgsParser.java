package com.volta.master.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.volta.model.TestConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.ApplicationArguments;

public class ArgsParser {

  private static final String USAGE =
      """
              Usage:   java -jar volta-master.jar --url=<url> --rps=<rps> --duration=<seconds> --agent=<host:port> [--agent=<host:port> ...]
                       java -jar volta-master.jar --config=./<config.json(.yaml/.yml)> --agent=<host:port> [--agent=<host:port> ...]

              Example: java -jar volta-master.jar --url=https://httpbin.org/get --rps=10 --duration=30 --agent=localhost:7070
                       java -jar volta-master.jar --url=https://httpbin.org/get --rps=100 --duration=30 --agent=localhost:7070 --agent=localhost:7071
                       java -jar volta-master.jar --config=./config.json --agent=localhost:7070 [--stats-out=report.jsonl|report.csv]
              """;

  public static MasterArgs parse(ApplicationArguments args) {

    TestConfig testConfig;
    if (args.containsOption("config")) {
      String configPath = requireString(args, "config");

      Path path = Path.of(configPath);
      if (!Files.exists(path)) {
        throw new ArgsException("Config file not found: " + configPath + "\n" + USAGE);
      }
      if (!Files.isRegularFile(path)) {
        throw new ArgsException("Config path is not a regular file: " + configPath + "\n" + USAGE);
      }

      ObjectMapper mapper;
      String name = path.getFileName().toString().toLowerCase();
      if (name.endsWith(".json")) {
        mapper = new ObjectMapper();
      } else if (name.endsWith(".yaml") || name.endsWith(".yml")) {
        mapper = new ObjectMapper(new YAMLFactory());
      } else {
        throw new ArgsException("Unsupported file format: " + configPath + "\n" + USAGE);
      }

      try {
        testConfig = mapper.readValue(path.toFile(), TestConfig.class);
      } catch (IOException e) {
        throw new ArgsException(
            "Failed to read/parse config: " + configPath + "\n" + e.getMessage() + "\n" + USAGE);
      }

      if (testConfig.rps() <= 0) {
        throw new ArgsException("Argument rps in config must be positive\n" + USAGE);
      }
      if (testConfig.duration() <= 0) {
        throw new ArgsException("Argument duration in config must be positive\n" + USAGE);
      }
      validateUrl(testConfig.url());

    } else {
      String url = requireString(args, "url");
      int rps = requirePositiveInt(args, "rps");
      int duration = requirePositiveInt(args, "duration");

      validateUrl(url);
      testConfig = new TestConfig(url, rps, duration);
    }
    List<String> agentUrls = requireAgents(args);

    if (args.containsOption("stats-out")) {
      String outputFile = requireString(args, "stats-out");
      return new MasterArgs(testConfig, Optional.of(outputFile), agentUrls);
    }

    return new MasterArgs(testConfig, Optional.empty(), agentUrls);
  }

  private static String requireString(ApplicationArguments args, String name) {
    if (!args.containsOption(name)) {
      throw new ArgsException("Missing required argument: --" + name + "\n" + USAGE);
    }
    String value = args.getOptionValues(name).get(0);
    if (value == null || value.isBlank()) {
      throw new ArgsException("Argument --" + name + " must not be blank\n" + USAGE);
    }
    return value;
  }

  private static int requirePositiveInt(ApplicationArguments args, String name) {
    String raw = requireString(args, name);
    int value;
    try {
      value = Integer.parseInt(raw);
    } catch (NumberFormatException e) {
      throw new ArgsException(
          "Argument --" + name + " must be an integer, got: " + raw + "\n" + USAGE);
    }
    if (value <= 0) {
      throw new ArgsException(
          "Argument --" + name + " must be positive, got: " + value + "\n" + USAGE);
    }
    return value;
  }

  private static void validateUrl(String url) {
    if (!url.startsWith("http://") && !url.startsWith("https://")) {
      throw new ArgsException(
          "Argument --url must start with http:// or https://, got: " + url + "\n" + USAGE);
    }
  }

  private static List<String> requireAgents(ApplicationArguments args) {
    if (!args.containsOption("agent")) {
      throw new ArgsException("Missing required argument: --agent\n" + USAGE);
    }

    List<String> rawAgents = args.getOptionValues("agent");
    if (rawAgents == null || rawAgents.isEmpty()) {
      throw new ArgsException("Missing required argument: --agent\n" + USAGE);
    }

    List<String> agentUrls = new ArrayList<>(rawAgents.size());
    for (String agent : rawAgents) {
      if (agent == null || agent.isBlank()) {
        throw new ArgsException("Argument --agent must not be blank\n" + USAGE);
      }
      validateAgent(agent);
      agentUrls.add("http://" + agent);
    }

    return List.copyOf(agentUrls);
  }

  private static void validateAgent(String agent) {
    String[] parts = agent.split(":");
    if (parts.length != 2) {
      throw new ArgsException(
          "Argument --agent must follow host:port format, got: " + agent + "\n" + USAGE);
    }
    int port;
    try {
      port = Integer.parseInt(parts[1]);
    } catch (NumberFormatException e) {
      throw new ArgsException(
          "Argument --agent port must be an integer, got: " + parts[1] + "\n" + USAGE);
    }
    if (port < 1 || port > 65535) {
      throw new ArgsException(
          "Argument --agent port must be in [1, 65535], got: " + port + "\n" + USAGE);
    }
  }
}
