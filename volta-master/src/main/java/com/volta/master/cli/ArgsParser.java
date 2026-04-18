package com.volta.master.cli;

import com.volta.model.TestConfig;
import org.springframework.boot.ApplicationArguments;

public class ArgsParser {
  private static final String USAGE =
      """
       Usage:   java -jar volta-master.jar --url=<url> --rps=<rps> --duration=<seconds> --agent=<host:port>
      Example: java -jar volta-master.jar --url=https://httpbin.org/get --rps=10 --duration=30 --agent=localhost:7070
      """;

  public static TestConfig parse(ApplicationArguments args) {
    String url = requireString(args, "url");
    int rps = requirePositiveInt(args, "rps");
    int duration = requirePositiveInt(args, "duration");
    String agent = requireString(args, "agent");

    validateUrl(url);
    validateAgent(agent);

    return new TestConfig(url, rps, duration, agent);
  }

  private static String requireString(ApplicationArguments args, String name) {
    if (!args.containsOption(name)) {
      throw new ArgsException("Missing required argument: --" + name + "\n" + USAGE);
    }
    String value = args.getOptionValues(name).getFirst();
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
