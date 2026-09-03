package com.example.webflux.client;

import com.example.webflux.exception.ExternalServiceException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalDiscountClientTest {

    private MockWebServer mockWebServer;
    private ExternalDiscountClient discountClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/api/v1/discounts").toString())
                .build();

        discountClient = new ExternalDiscountClient(webClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("Successfully fetch discount from external client using StepVerifier")
    void getDiscountSuccess() {
        String jsonResponseBody = """
                {
                  "code": "SUMMER2026",
                  "percentage": 15.0,
                  "valid": true
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonResponseBody));

        StepVerifier.create(discountClient.getDiscount("SUMMER2026"))
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo("SUMMER2026");
                    assertThat(response.getPercentage()).isEqualByComparingTo("15.0");
                    assertThat(response.getValid()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should throw ExternalServiceException on 500 error")
    void getDiscountServerError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        StepVerifier.create(discountClient.getDiscount("FAIL500"))
                .expectError(ExternalServiceException.class)
                .verify();
    }
}
