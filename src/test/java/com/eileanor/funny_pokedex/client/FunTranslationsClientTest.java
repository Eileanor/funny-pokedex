package com.eileanor.funny_pokedex.client;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FunTranslationsClientTest {

    MockWebServer server;
    FunTranslationsClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        WebClient webClient = WebClient.builder()
                .baseUrl(server.url("/").toString())
                .build();
        client = new FunTranslationsClient(webClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void translateShakespeare_happyPath_returnsTranslatedText() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "success": {"total": 1},
                          "contents": {"translated": "Thou art a strange seed, verily."}
                        }
                        """));

        Optional<String> result = client.translateShakespeare("A strange seed.");

        assertThat(result).contains("Thou art a strange seed, verily.");
    }

    @Test
    void translate_successTotalZero_returnsEmpty() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "success": {"total": 0},
                          "contents": {"translated": ""}
                        }
                        """));

        Optional<String> result = client.translateYoda("Some text.");

        assertThat(result).isEmpty();
    }

    @Test
    void translate_nullContents_returnsEmpty() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "success": {"total": 1},
                          "contents": null
                        }
                        """));

        Optional<String> result = client.translateShakespeare("Some text.");

        assertThat(result).isEmpty();
    }
}
