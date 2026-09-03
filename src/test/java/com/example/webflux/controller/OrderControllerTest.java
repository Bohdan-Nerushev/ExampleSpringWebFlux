package com.example.webflux.controller;

import com.example.webflux.domain.enums.OrderStatus;
import com.example.webflux.dto.request.CreateOrderItemRequest;
import com.example.webflux.dto.request.CreateOrderRequest;
import com.example.webflux.dto.response.OrderItemResponse;
import com.example.webflux.dto.response.OrderResponse;
import com.example.webflux.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    private WebTestClient webTestClient;

    @Mock
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        OrderController orderController = new OrderController(orderService);
        webTestClient = WebTestClient.bindToController(orderController).build();
    }

    @Test
    @DisplayName("POST /api/v1/orders - Successfully create order")
    void createOrderSuccess() {
        CreateOrderItemRequest itemRequest = new CreateOrderItemRequest("PROD-100", 2, new BigDecimal("50.00"));
        CreateOrderRequest request = new CreateOrderRequest("CUST-1", "SUMMER2026", List.of(itemRequest));

        OrderItemResponse itemResponse = OrderItemResponse.builder()
                .id(10L)
                .productId("PROD-100")
                .quantity(2)
                .price(new BigDecimal("50.00"))
                .build();

        OrderResponse response = OrderResponse.builder()
                .id(1L)
                .customerId("CUST-1")
                .status(OrderStatus.NEW)
                .totalAmount(new BigDecimal("85.00"))
                .discountAmount(new BigDecimal("15.00"))
                .items(List.of(itemResponse))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(Mono.just(response));

        webTestClient.post()
                .uri("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.customerId").isEqualTo("CUST-1")
                .jsonPath("$.status").isEqualTo("NEW")
                .jsonPath("$.totalAmount").isEqualTo(85.00)
                .jsonPath("$.items[0].productId").isEqualTo("PROD-100");
    }

    @Test
    @DisplayName("GET /api/v1/orders/{id} - Successfully return order")
    void getOrderByIdSuccess() {
        OrderResponse response = OrderResponse.builder()
                .id(1L)
                .customerId("CUST-1")
                .status(OrderStatus.NEW)
                .totalAmount(new BigDecimal("100.00"))
                .discountAmount(BigDecimal.ZERO)
                .items(List.of())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(orderService.getOrderById(1L)).thenReturn(Mono.just(response));

        webTestClient.get()
                .uri("/api/v1/orders/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.customerId").isEqualTo("CUST-1");
    }

    @Test
    @DisplayName("GET /api/v1/orders - Return Flux of orders")
    void getAllOrdersSuccess() {
        OrderResponse response1 = OrderResponse.builder().id(1L).customerId("CUST-1").items(List.of()).build();
        OrderResponse response2 = OrderResponse.builder().id(2L).customerId("CUST-2").items(List.of()).build();

        when(orderService.getAllOrders()).thenReturn(Flux.just(response1, response2));

        webTestClient.get()
                .uri("/api/v1/orders")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].id").isEqualTo(1)
                .jsonPath("$[1].id").isEqualTo(2);
    }
}
