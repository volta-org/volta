package com.volta.agent.http;

import com.volta.model.RequestSpec;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HttpSender implements AutoCloseable {
  private final HttpClient client = HttpClient.newHttpClient();

  public HttpResponse<String> send(RequestSpec spec) throws IOException, InterruptedException {
    HttpRequest request = HttpRequestBuilder.build(spec);
    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  public HttpResponse<String> send(String url) throws IOException, InterruptedException {
    return send(RequestSpec.get(url));
  }

  @Override
  public void close() throws Exception {
    client.close();
  }
}
