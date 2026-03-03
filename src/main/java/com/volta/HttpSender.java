package com.volta;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HttpSender implements AutoCloseable {
  private final HttpClient client = HttpClient.newHttpClient();

  public HttpResponse<String> send(String url) throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  @Override
  public void close() throws Exception {
    client.close();
  }
}
