package com.volta.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ScenarioValidatorTest {

  @Test
  void acceptsValidGetRequest() {
    TestConfig config = TestConfig.ofGet("https://example.com", 10, 30);

    assertDoesNotThrow(() -> ScenarioValidator.validate(config));
  }

  @Test
  void acceptsPostWithJsonBody() {
    RequestSpec request =
        new RequestSpec(
            HttpMethod.POST,
            "https://example.com/api",
            java.util.Map.of("Content-Type", "application/json"),
            "{\"ok\":true}");

    TestConfig config = new TestConfig(5, 10, request);

    assertDoesNotThrow(() -> ScenarioValidator.validate(config));
  }

  @Test
  void rejectsMissingUrl() {
    RequestSpec request = new RequestSpec(HttpMethod.GET, "  ", null, null);
    TestConfig config = new TestConfig(5, 10, request);

    IllegalArgumentException error =
        assertThrows(IllegalArgumentException.class, () -> ScenarioValidator.validate(config));

    assertTrue(error.getMessage().contains("URL is required"));
  }

  @Test
  void rejectsUnsupportedProtocol() {
    TestConfig config = TestConfig.ofGet("ftp://example.com", 5, 10);

    IllegalArgumentException error =
        assertThrows(IllegalArgumentException.class, () -> ScenarioValidator.validate(config));

    assertTrue(error.getMessage().contains("http://"));
  }

  @Test
  void rejectsBodyForGet() {
    RequestSpec request = new RequestSpec(HttpMethod.GET, "https://example.com", null, "{\"x\":1}");
    TestConfig config = new TestConfig(5, 10, request);

    IllegalArgumentException error =
        assertThrows(IllegalArgumentException.class, () -> ScenarioValidator.validate(config));

    assertTrue(error.getMessage().contains("Body is not allowed"));
  }

  @Test
  void rejectsUnsupportedHttpMethod() {
    assertThrows(IllegalArgumentException.class, () -> HttpMethod.fromString("TRACE"));
  }
}
