package com.volta.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Path;

public final class TestConfigReader {

  private TestConfigReader() {}

  public static TestConfig read(Path path) throws IOException {
    ObjectMapper mapper = mapperFor(path);
    TestConfig config = mapper.readValue(path.toFile(), TestConfig.class);
    ScenarioValidator.validate(config);
    return config;
  }

  public static TestConfig readJson(String json) throws IOException {
    TestConfig config = new ObjectMapper().readValue(json, TestConfig.class);
    ScenarioValidator.validate(config);
    return config;
  }

  private static ObjectMapper mapperFor(Path path) {
    String name = path.getFileName().toString().toLowerCase();
    if (name.endsWith(".yaml") || name.endsWith(".yml")) {
      return new ObjectMapper(new YAMLFactory());
    }
    return new ObjectMapper();
  }
}
