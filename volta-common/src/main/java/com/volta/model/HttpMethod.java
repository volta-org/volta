package com.volta.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

public enum HttpMethod {
  GET,
  POST,
  PUT,
  PATCH,
  DELETE;

  public boolean allowsBody() {
    return this == POST || this == PUT || this == PATCH;
  }

  @JsonCreator
  public static HttpMethod fromString(String value) {
    if (value == null || value.isBlank()) {
      return GET;
    }
    try {
      return HttpMethod.valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Unsupported HTTP method: " + value);
    }
  }
}
