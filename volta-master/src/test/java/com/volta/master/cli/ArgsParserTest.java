package com.volta.master.cli;

import static org.junit.jupiter.api.Assertions.*;

import com.volta.model.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

class CliArgsParserTest {

  private static DefaultApplicationArguments args(String... rawArgs) {
    return new DefaultApplicationArguments(rawArgs);
  }

  @Test
  void validArgsProduceCorrectMasterArgs() {
    MasterArgs result =
        ArgsParser.parse(
            args(
                "--url=https://httpbin.org/get",
                "--rps=10",
                "--duration=30",
                "--agent=localhost:7070"));

    TestConfig config = result.testConfig();
    assertEquals("https://httpbin.org/get", config.url());
    assertEquals(10, config.rps());
    assertEquals(30, config.duration());
    assertEquals("http://localhost:7070", result.agentUrl());
  }

  @Test
  void httpUrlIsAccepted() {
    assertDoesNotThrow(
        () ->
            ArgsParser.parse(
                args(
                    "--url=http://example.com",
                    "--rps=5",
                    "--duration=10",
                    "--agent=localhost:7070")));
  }

  @Test
  void httpsUrlIsAccepted() {
    assertDoesNotThrow(
        () ->
            ArgsParser.parse(
                args(
                    "--url=https://example.com",
                    "--rps=5",
                    "--duration=10",
                    "--agent=localhost:7070")));
  }

  @Test
  void portBoundary1IsAccepted() {
    assertDoesNotThrow(
        () ->
            ArgsParser.parse(
                args(
                    "--url=https://example.com",
                    "--rps=5",
                    "--duration=10",
                    "--agent=localhost:1")));
  }

  @Test
  void portBoundary65535IsAccepted() {
    assertDoesNotThrow(
        () ->
            ArgsParser.parse(
                args(
                    "--url=https://example.com",
                    "--rps=5",
                    "--duration=10",
                    "--agent=localhost:65535")));
  }

  @Test
  void missingUrlThrows() {
    assertThrows(
        ArgsException.class,
        () -> ArgsParser.parse(args("--rps=10", "--duration=30", "--agent=localhost:7070")));
  }

  @Test
  void missingRpsThrows() {
    assertThrows(
        ArgsException.class,
        () ->
            ArgsParser.parse(
                args("--url=https://example.com", "--duration=30", "--agent=localhost:7070")));
  }

  @Test
  void missingDurationThrows() {
    assertThrows(
        ArgsException.class,
        () ->
            ArgsParser.parse(
                args("--url=https://example.com", "--rps=10", "--agent=localhost:7070")));
  }

  @Test
  void missingAgentThrows() {
    assertThrows(
        ArgsException.class,
        () -> ArgsParser.parse(args("--url=https://example.com", "--rps=10", "--duration=30")));
  }

  @Test
  void blankUrlThrows() {
    assertThrows(
        ArgsException.class,
        () ->
            ArgsParser.parse(
                args("--url=", "--rps=10", "--duration=30", "--agent=localhost:7070")));
  }

  @Test
  void urlWithoutProtocolThrows() {
    assertThrows(
        ArgsException.class,
        () ->
            ArgsParser.parse(
                args("--url=example.com", "--rps=10", "--duration=30", "--agent=localhost:7070")));
  }

  @Test
  void ftpUrlThrows() {
    assertThrows(
        ArgsException.class,
        () ->
            ArgsParser.parse(
                args(
                    "--url=ftp://example.com",
                    "--rps=10",
                    "--duration=30",
                    "--agent=localhost:7070")));
  }

  @Test
  void zeroRpsThrows() {
    assertThrows(
        ArgsException.class,
        () ->
            ArgsParser.parse(
                args(
                    "--url=https://example.com",
                    "--rps=0",
                    "--duration=30",
                    "--agent=localhost:7070")));
  }

  @Test
  void negativeRpsThrows() {
    assertThrows(
        ArgsException.class,
        () ->
            ArgsParser.parse(
                args(
                    "--url=https://example.com",
                    "--rps=-1",
                    "--duration=30",
                    "--agent=localhost:7070")));
  }

  @Test
  void nonNumericRpsThrows() {
    assertThrows(
        ArgsException.class,
        () ->
            ArgsParser.parse(
                args(
                    "--url=https://example.com",
                    "--rps=abc",
                    "--duration=30",
                    "--agent=localhost:7070")));
  }

  @Test
  void zeroDurationThrows() {
    assertThrows(
        ArgsException.class,
        () ->
            ArgsParser.parse(
                args(
                    "--url=https://example.com",
                    "--rps=10",
                    "--duration=0",
                    "--agent=localhost:7070")));
  }

  @Test
  void negativeDurationThrows() {
    assertThrows(
        ArgsException.class,
        () ->
            ArgsParser.parse(
                args(
                    "--url=https://example.com",
                    "--rps=10",
                    "--duration=-5",
                    "--agent=localhost:7070")));
  }

  @Test
  void agentWithoutPortThrows() {
    assertThrows(
        ArgsException.class,
        () ->
            ArgsParser.parse(
                args(
                    "--url=https://example.com",
                    "--rps=10",
                    "--duration=30",
                    "--agent=localhost")));
  }

  @Test
  void agentPortZeroThrows() {
    assertThrows(
        ArgsException.class,
        () ->
            ArgsParser.parse(
                args(
                    "--url=https://example.com",
                    "--rps=10",
                    "--duration=30",
                    "--agent=localhost:0")));
  }

  @Test
  void agentPortAbove65535Throws() {
    assertThrows(
        ArgsException.class,
        () ->
            ArgsParser.parse(
                args(
                    "--url=https://example.com",
                    "--rps=10",
                    "--duration=30",
                    "--agent=localhost:65536")));
  }

  @Test
  void agentPortNonNumericThrows() {
    assertThrows(
        ArgsException.class,
        () ->
            ArgsParser.parse(
                args(
                    "--url=https://example.com",
                    "--rps=10",
                    "--duration=30",
                    "--agent=localhost:abc")));
  }
}
