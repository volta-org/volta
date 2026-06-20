package com.volta.master.reporter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.volta.master.StartupRunner;
import com.volta.master.client.AgentClient;
import com.volta.stats.StatsSnapshot;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StatsReporter {
  private static final Logger log = LoggerFactory.getLogger(StartupRunner.class);

  private static final Locale CSV_LOCALE = Locale.ROOT;

  private static final String CSV_HEADER =
      "kind,elapsedSeconds,totalRequests,successCount,errorCount,avgLatencyMs,minLatencyMs,maxLatencyMs,successRatePercent,cumulativeAvgRps";

  // ANSI-codes for colors
  public static final String RESET = "\u001B[0m";
  public static final String GREEN = "\u001B[32m";
  public static final String RED = "\u001B[31m";
  public static final String YELLOW = "\u001B[33m";
  public static final String BLUE = "\u001B[34m";
  public static final String CYAN = "\u001B[36m";
  public static final String BOLD = "\u001B[1m";

  private enum StatsFileFormat {
    JSONL,
    CSV
  }

  private record StatsFileSink(BufferedWriter writer, StatsFileFormat format) {}

  private final AgentClient agentClient;
  private final ObjectMapper jsonMapper = new ObjectMapper();

  public StatsReporter(AgentClient agentClient) {
    this.agentClient = agentClient;
  }

  public void startReporting(
      String agentUrl, int durationSeconds, Optional<String> outputFilePath) {

    log.info("Starting live stats reporting for {}s", durationSeconds);

    Optional<StatsFileSink> sink = openOptionalSink(outputFilePath);
    try {
      runLoop(agentUrl, durationSeconds, sink);
      try {
        StatsSnapshot finalStats = agentClient.getStats(agentUrl);
        writeFinalToFile(sink, finalStats);
        printFinalStats(finalStats);
      } catch (Exception e) {
        log.error("Failed to fetch final stats: {}", e.getMessage());
      }
    } finally {
      if (sink.isPresent()) {
        try {
          sink.get().writer().close();
        } catch (IOException e) {
          log.warn("Failed to close stats file: {}", e.getMessage());
        }
      }
    }
  }

  private static StatsFileFormat detectFormat(Path path) {
    String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
    if (name.endsWith(".csv")) {
      return StatsFileFormat.CSV;
    }
    return StatsFileFormat.JSONL;
  }

  private Optional<StatsFileSink> openOptionalSink(Optional<String> outputFilePath) {
    if (outputFilePath.isEmpty()) {
      return Optional.empty();
    }
    Path path = Path.of(outputFilePath.get());
    try {
      Path parent = path.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      BufferedWriter writer =
          Files.newBufferedWriter(
              path,
              StandardCharsets.UTF_8,
              StandardOpenOption.CREATE,
              StandardOpenOption.TRUNCATE_EXISTING,
              StandardOpenOption.WRITE);
      StatsFileFormat format = detectFormat(path);
      if (format == StatsFileFormat.CSV) {
        writer.write(CSV_HEADER);
        writer.newLine();
        writer.flush();
      }
      return Optional.of(new StatsFileSink(writer, format));
    } catch (IOException e) {
      log.error("Failed to open stats output '{}', continuing without file", path, e);
      return Optional.empty();
    }
  }

  private void runLoop(String agentUrl, int durationSeconds, Optional<StatsFileSink> sink) {

    for (int i = 0; i < durationSeconds; i++) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.warn("Stats reporting interrupted");
        break;
      }

      try {
        StatsSnapshot stats = agentClient.getStats(agentUrl);
        int elapsedSeconds = i + 1;
        printLiveStats(stats, elapsedSeconds);
        writeSampleToFile(sink, stats, elapsedSeconds);
      } catch (Exception e) {
        log.error("Failed to fetch stats: {}", e.getMessage());
      }
    }
  }

  private void writeSampleToFile(
      Optional<StatsFileSink> sink, StatsSnapshot stats, int elapsedSeconds) {

    if (sink.isEmpty()) {
      return;
    }
    StatsFileSink s = sink.get();
    try {
      if (s.format() == StatsFileFormat.JSONL) {
        jsonLineOut(s.writer(), stats);
        return;
      }
      csvRowSample(s.writer(), stats, elapsedSeconds);
    } catch (IOException e) {
      log.warn("Failed to write stats line: {}", e.getMessage());
    }
  }

  private void writeFinalToFile(Optional<StatsFileSink> sink, StatsSnapshot stats) {

    if (sink.isEmpty()) {
      return;
    }
    StatsFileSink s = sink.get();
    try {
      if (s.format() == StatsFileFormat.JSONL) {
        jsonLineOut(s.writer(), stats);
        return;
      }
      csvRowFinal(s.writer(), stats);
    } catch (IOException e) {
      log.warn("Failed to write stats line: {}", e.getMessage());
    }
  }

  private void jsonLineOut(BufferedWriter writer, StatsSnapshot stats) throws IOException {
    writer.write(jsonMapper.writeValueAsString(stats));
    writer.newLine();
    writer.flush();
  }

  private void csvRowSample(BufferedWriter writer, StatsSnapshot stats, int elapsedSeconds)
      throws IOException {
    writer.write(csvLineSample(stats, elapsedSeconds));
    writer.newLine();
    writer.flush();
  }

  private void csvRowFinal(BufferedWriter writer, StatsSnapshot stats) throws IOException {
    writer.write(csvLineFinal(stats));
    writer.newLine();
    writer.flush();
  }

  private String csvLineSample(StatsSnapshot stats, int elapsedSeconds) {

    double successRate = calculateSuccessRate(stats);
    long cumulativeAvgRps = calculateCurrentRps(stats, elapsedSeconds);
    return String.format(
        CSV_LOCALE,
        "sample,%d,%d,%d,%d,%s,%d,%d,%s,%d",
        elapsedSeconds,
        stats.totalRequests(),
        stats.successCount(),
        stats.errorCount(),
        formatDoubleCsv(stats.avgLatencyMs()),
        stats.minLatencyMs(),
        stats.maxLatencyMs(),
        formatDoubleCsv(successRate),
        cumulativeAvgRps);
  }

  private String csvLineFinal(StatsSnapshot stats) {

    double successRate = calculateSuccessRate(stats);
    return String.format(
        CSV_LOCALE,
        "final,,%d,%d,%d,%s,%d,%d,%s,",
        stats.totalRequests(),
        stats.successCount(),
        stats.errorCount(),
        formatDoubleCsv(stats.avgLatencyMs()),
        stats.minLatencyMs(),
        stats.maxLatencyMs(),
        formatDoubleCsv(successRate));
  }

  private static String formatDoubleCsv(double value) {

    return String.format(CSV_LOCALE, "%.4f", value);
  }

  private void printLiveStats(StatsSnapshot stats, int elapsedSeconds) {

    double successRate = calculateSuccessRate(stats);
    long currentRps = calculateCurrentRps(stats, elapsedSeconds);

    String rateColor = (successRate >= 100.0) ? GREEN : (successRate > 95.0 ? YELLOW : RED);
    String errorColor = (stats.errorCount() > 0) ? RED : RESET;

    String line =
        String.format(
            "[%sRPS: %d%s | %sSuccess: %.1f%%%s | %sAvg: %.0fms%s | %sErrors: %d%s]",
            CYAN,
            currentRps,
            RESET,
            rateColor,
            successRate,
            RESET,
            BLUE,
            stats.avgLatencyMs(),
            RESET,
            errorColor,
            stats.errorCount(),
            RESET);

    System.out.println(line);
  }

  private void printFinalStats(StatsSnapshot finalStats) {

    System.out.println("\n" + BOLD + CYAN + "========= FINAL STATS =========" + RESET);

    System.out.printf("Total Requests:  %s%s%d%s\n", BOLD, BLUE, finalStats.totalRequests(), RESET);
    System.out.printf("Success:         %s%s%d%s\n", BOLD, GREEN, finalStats.successCount(), RESET);
    System.out.printf("Errors:          %s%s%d%s\n", BOLD, RED, finalStats.errorCount(), RESET);

    System.out.printf(
        "Success Rate:    %s%.2f%%%s\n", BOLD, calculateSuccessRate(finalStats), RESET);
    System.out.printf(
        "Avg Latency:     %s%s%.2fms%s\n", BOLD, YELLOW, finalStats.avgLatencyMs(), RESET);

    System.out.printf("Min Latency:     %dms\n", finalStats.minLatencyMs());
    System.out.printf("Max Latency:     %dms\n", finalStats.maxLatencyMs());

    System.out.println(BOLD + CYAN + "===============================" + RESET);
  }

  private double calculateSuccessRate(StatsSnapshot stats) {

    if (stats.totalRequests() == 0) {
      return 0.0;
    }
    return (stats.successCount() * 100.0) / stats.totalRequests();
  }

  private long calculateCurrentRps(StatsSnapshot stats, int elapsedSeconds) {

    if (elapsedSeconds == 0) {
      return 0;
    }
    return stats.totalRequests() / elapsedSeconds;
  }
}
