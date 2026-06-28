package com.volta.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;

public class TestConfigDeserializer extends JsonDeserializer<TestConfig> {

  @Override
  public TestConfig deserialize(JsonParser parser, DeserializationContext context)
      throws IOException {
    JsonNode root = parser.getCodec().readTree(parser);

    int rps = root.get("rps").asInt();
    int duration = root.get("duration").asInt();

    RequestSpec request;
    if (root.has("request")) {
      request = context.readTreeAsValue(root.get("request"), RequestSpec.class);
    } else if (root.has("url")) {
      request = RequestSpec.get(root.get("url").asText());
    } else {
      throw new IllegalArgumentException("Config must contain 'request' or 'url'");
    }

    return new TestConfig(rps, duration, request);
  }
}
