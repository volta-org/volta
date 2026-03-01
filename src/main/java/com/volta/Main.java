package com.volta;

import java.io.IOException;
import java.net.http.HttpResponse;

public class Main {
    public static void main(String[] args) {
        HttpSender sender = new HttpSender();

        String URL = "https://jsonplaceholder.typicode.com/posts/1";

        while (true) {
            try {
                Thread.sleep(1000);
                HttpResponse<String> response = sender.send(URL);
                System.out.println("Status: %d, Body: %s".formatted(response.statusCode(), response.body()));
            } catch (Exception e) {
                System.out.println("Exception caught");
            }
        }
    }
}
