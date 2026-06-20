package com.volta.master.cli;

import com.volta.model.TestConfig;
import java.util.Optional;

public record MasterArgs(TestConfig testConfig, Optional<String> outputFile, String agentUrl) {}
