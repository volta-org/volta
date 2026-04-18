package com.volta.master.cli;

import com.volta.model.TestConfig;

public record MasterArgs(TestConfig testConfig, String agentUrl) {}
