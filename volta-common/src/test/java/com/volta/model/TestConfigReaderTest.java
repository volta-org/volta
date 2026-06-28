package com.volta.model;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TestConfigReaderTest {

  @Test
  void readsLegacyTopLevelUrlAsGet(@TempDir Path tempDir) throws Exception {
    Path config = tempDir.resolve("config.json");
    Files.writeString(
        config,
        """
        {
          "url": "https://httpbin.org/get",
          "rps": 10,
          "duration": 30
        }
        """);

    TestConfig result = TestConfigReader.read(config);

    assertEquals(10, result.rps());
    assertEquals(30, result.duration());
    assertEquals(HttpMethod.GET, result.request().method());
    assertEquals("https://httpbin.org/get", result.request().url());
  }

  @Test
  void readsPostScenarioFromJson(@TempDir Path tempDir) throws Exception {
    Path config = tempDir.resolve("config.json");
    Files.writeString(
        config,
        """
        {
          "rps": 5,
          "duration": 10,
          "request": {
            "method": "POST",
            "url": "https://httpbin.org/post",
            "headers": {
              "Content-Type": "application/json"
            },
            "body": "{\\"name\\":\\"volta\\"}"
          }
        }
        """);

    TestConfig result = TestConfigReader.read(config);

    assertEquals(HttpMethod.POST, result.request().method());
    assertEquals("application/json", result.request().headers().get("Content-Type"));
    assertEquals("{\"name\":\"volta\"}", result.request().body());
  }

  @Test
  void readsPutScenarioFromYaml(@TempDir Path tempDir) throws Exception {
    Path config = tempDir.resolve("config.yaml");
    Files.writeString(
        config,
        """
        rps: 5
        duration: 10
        request:
          method: PUT
          url: "https://httpbin.org/put"
          headers:
            Content-Type: application/json
          body: '{"id":1}'
        """);

    TestConfig result = TestConfigReader.read(config);

    assertEquals(HttpMethod.PUT, result.request().method());
    assertEquals("https://httpbin.org/put", result.request().url());
    assertEquals("{\"id\":1}", result.request().body());
  }

  @Test
  void rejectsConfigWithoutUrlOrRequest(@TempDir Path tempDir) throws Exception {
    Path config = tempDir.resolve("config.json");
    Files.writeString(
        config,
        """
        {
          "rps": 5,
          "duration": 10
        }
        """);

    assertThrows(IllegalArgumentException.class, () -> TestConfigReader.read(config));
  }

  @Test
  void rejectsGetWithBodyInJsonString() {
    String json =
        """
        {
          "rps": 5,
          "duration": 10,
          "request": {
            "method": "GET",
            "url": "https://example.com",
            "body": "not allowed"
          }
        }
        """;

    assertThrows(IllegalArgumentException.class, () -> TestConfigReader.readJson(json));
  }
}
