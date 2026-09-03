package com.example.webflux.exception;

import com.example.webflux.controller.OrderController;
import com.example.webflux.dto.request.CreateOrderRequest;
import com.example.webflux.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private WebTestClient webTestClient;

    @Mock
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        OrderController orderController = new OrderController(orderService);
        webTestClient = WebTestClient.bindToController(orderController)
                .controllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Should return 400 Bad Request on validation error")
    void handleValidationError() {
        CreateOrderRequest invalidRequest = new CreateOrderRequest("", "", List.of());

        webTestClient.post()
                .uri("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.error").isEqualTo("Bad Request")
                .jsonPath("$.message").isEqualTo("Validation failed for input payload")
                .jsonPath("$.details").isArray();
    }

    @Test
    @DisplayName("Should return 404 Not Found on OrderNotFoundException")
    void handleOrderNotFoundException() {
        when(orderService.getOrderById(99L)).thenReturn(Mono.error(new OrderNotFoundException(99L)));

        webTestClient.get()
                .uri("/api/v1/orders/99")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.error").isEqualTo("Not Found")
                .jsonPath("$.message").isEqualTo("Order not found with id: 99");
    }

    @Test
    @DisplayName("Should return 502 Bad Gateway on ExternalServiceException")
    void handleExternalServiceException() {
        when(orderService.getOrderById(1L)).thenReturn(Mono.error(new ExternalServiceException("Service unavailable")));

        webTestClient.get()
                .uri("/api/v1/orders/1")
                .exchange()
                .expectStatus().isEqualTo(502)
                .expectBody()
                .jsonPath("$.status").isEqualTo(502)
                .jsonPath("$.error").isEqualTo("Bad Gateway")
                .jsonPath("$.message").isEqualTo("Service unavailable");
    }

    @Test
    @DisplayName("Should return 500 Internal Server Error on unexpected Exception")
    void handleGeneralException() {
        when(orderService.getOrderById(1L)).thenReturn(Mono.error(new RuntimeException("Database timeout")));

        webTestClient.get()
                .uri("/api/v1/orders/1")
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.status").isEqualTo(500)
                .jsonPath("$.error").isEqualTo("Internal Server Error")
                .jsonPath("$.message").isEqualTo("An unexpected internal server error occurred");
    }
}
