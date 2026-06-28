package com.volta.model;

public final class ScenarioValidator {

  private ScenarioValidator() {}

  public static void validate(TestConfig config) {
    if (config.rps() <= 0) {
      throw new IllegalArgumentException("RPS must be positive");
    }
    if (config.duration() <= 0) {
      throw new IllegalArgumentException("Duration must be positive");
    }
    validateRequest(config.request());
  }

  public static void validateRequest(RequestSpec request) {
    if (request.url() == null || request.url().isBlank()) {
      throw new IllegalArgumentException("URL is required");
    }
    if (!request.url().startsWith("http://") && !request.url().startsWith("https://")) {
      throw new IllegalArgumentException("URL must start with http:// or https://");
    }
    if (!request.method().allowsBody() && hasBody(request)) {
      throw new IllegalArgumentException("Body is not allowed for " + request.method());
    }
  }

  private static boolean hasBody(RequestSpec request) {
    return request.body() != null && !request.body().isBlank();
  }
}
