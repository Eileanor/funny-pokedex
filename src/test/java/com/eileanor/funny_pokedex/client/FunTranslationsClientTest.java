package com.eileanor.funny_pokedex.client;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import com.eileanor.funny_pokedex.config.FunTranslationsProperties;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

class FunTranslationsClientTest {

    MockWebServer server;
    FunTranslationsClient client;
    FunTranslationsProperties properties;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        WebClient webClient = WebClient.builder()
                .baseUrl(server.url("/").toString())
                .build();
        properties = new FunTranslationsProperties("", "/shakespeare", "/yoda");
        client = new FunTranslationsClient(webClient, properties);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    @DisplayName("translateShakespeare returns translated text for valid input")
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
    @DisplayName("translateYoda returns translated text for valid input")
    void translateYoda_happyPath_returnsTranslatedText() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "success": {"total": 1},
                          "contents": {"translated": "A strange seed, it is."}
                        }
                        """));

        Optional<String> result = client.translateYoda("A strange seed.");

        assertThat(result).contains("A strange seed, it is.");
    }

    @Test
    @DisplayName("translate returns empty Optional when success total is zero")
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
    @DisplayName("translate returns empty Optional when contents is null")
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

    @Test
    @DisplayName("translate returns empty Optional when success is null")
    void translate_nullSuccess_returnsEmpty() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "success": null,
                          "contents": {"translated": "Nothing to see here."}
                        }
                        """));

        Optional<String> result = client.translateYoda("Some text.");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("translate returns empty Optional when response body is null")
    void translate_nullResponse_returnsEmpty() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("null"));

        Optional<String> result = client.translateShakespeare("Some text.");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("emptyFallback returns empty Optional")
    void emptyFallback_returnsEmptyOptional() throws Exception {
        var method = FunTranslationsClient.class.getDeclaredMethod("emptyFallback", String.class, Throwable.class);
        method.setAccessible(true);

        Optional<String> result = (Optional<String>) method.invoke(client, "Some text.", new RuntimeException("boom"));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("translate throws RuntimeException on HTTP error")
    void translate_httpError_throwsRuntimeException() {
        server.enqueue(new MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "error": {"code": 500, "message": "Internal Server Error"}
                        }
                        """));

        assertThrows(RuntimeException.class, () -> client.translateYoda("Some text."));
    }
}
