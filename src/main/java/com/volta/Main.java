package com.volta;

import com.volta.engine.LoadEngine;

public class Main {
  private static final String TARGET_URL = "https://jsonplaceholder.typicode.com/posts/1";
  private static final int TARGET_RPS = 5;
  private static final int DURATION_SECONDS = 10;

  public static void main(String[] args) {
    LoadEngine engine = new LoadEngine(TARGET_URL, TARGET_RPS, DURATION_SECONDS);
    engine.start();
  }
}
