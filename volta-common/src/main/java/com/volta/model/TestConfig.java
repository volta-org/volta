package com.volta.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = TestConfigDeserializer.class)
public record TestConfig(int rps, int duration, RequestSpec request) {

  public String url() {
    return request.url();
  }

  public static TestConfig ofGet(String url, int rps, int duration) {
    return new TestConfig(rps, duration, RequestSpec.get(url));
  }
}
