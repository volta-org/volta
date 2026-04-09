package com.volta.stats;

public record StatsSnapshot(
    long totalRequests,
    long successCount,
    long errorCount,
    double avgLatencyMs,
    long minLatencyMs,
    long maxLatencyMs) {}
