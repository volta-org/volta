package com.volta;

import org.junit.jupiter.api.Test;
import java.net.http.HttpResponse;
import static org.junit.jupiter.api.Assertions.*;

class HttpSenderTest {

    private static final String TEST_URL = "https://jsonplaceholder.typicode.com/posts/1";
    private static final String INVALID_URL = "https://thisdomaindoesnotexist12345.com";

    private final HttpSender sender = new HttpSender();

    @Test
    void sendReturns200ForValidUrl() throws Exception {
        HttpResponse<String> response = sender.send(TEST_URL);
        assertEquals(200, response.statusCode());
    }

    @Test
    void responseBodyIsNotEmpty() throws Exception {
        HttpResponse<String> response = sender.send(TEST_URL);
        assertFalse(response.body().isEmpty());
    }

    @Test
    void sendThrowsOnInvalidUrl() {
        assertThrows(Exception.class, () -> sender.send(INVALID_URL));
    }
}
