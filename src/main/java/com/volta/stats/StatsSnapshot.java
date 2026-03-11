package com.volta.stats;

public record StatsSnapshot(
    long totalRequests,
    long successCount,
    long errorCount,
    long avgLatencyMs,
    long minLatencyMs,
    long maxLatencyMs) {}
