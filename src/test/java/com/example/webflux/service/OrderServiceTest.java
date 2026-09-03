package com.example.webflux.service;

import com.example.webflux.client.ExternalDiscountClient;
import com.example.webflux.client.ExternalNotificationClient;
import com.example.webflux.domain.entity.Order;
import com.example.webflux.domain.entity.OrderItem;
import com.example.webflux.domain.enums.OrderStatus;
import com.example.webflux.dto.request.CreateOrderItemRequest;
import com.example.webflux.dto.request.CreateOrderRequest;
import com.example.webflux.dto.response.DiscountResponse;
import com.example.webflux.dto.response.NotificationResponse;
import com.example.webflux.exception.ExternalServiceException;
import com.example.webflux.exception.OrderNotFoundException;
import com.example.webflux.mapper.OrderMapper;
import com.example.webflux.repository.OrderItemRepository;
import com.example.webflux.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ExternalDiscountClient discountClient;

    @Mock
    private ExternalNotificationClient notificationClient;

    @Spy
    private OrderMapper orderMapper = new OrderMapper();

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    @DisplayName("Create order successfully with discount applied")
    void createOrderSuccessWithDiscount() {
        CreateOrderItemRequest itemRequest = new CreateOrderItemRequest("PROD-1", 2, new BigDecimal("50.00"));
        CreateOrderRequest request = new CreateOrderRequest("CUST-1", "SUMMER2026", List.of(itemRequest));

        Order savedOrder = Order.builder()
                .id(1L)
                .customerId("CUST-1")
                .status(OrderStatus.NEW)
                .totalAmount(new BigDecimal("85.00"))
                .discountAmount(new BigDecimal("15.00"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        OrderItem savedItem = OrderItem.builder()
                .id(10L)
                .orderId(1L)
                .productId("PROD-1")
                .quantity(2)
                .price(new BigDecimal("50.00"))
                .build();

        when(discountClient.getDiscount("SUMMER2026"))
                .thenReturn(Mono.just(new DiscountResponse("SUMMER2026", new BigDecimal("15.0"), true)));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(savedOrder));
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(Mono.just(savedItem));
        when(notificationClient.sendNotification(anyLong(), any(), any()))
                .thenReturn(Mono.just(new NotificationResponse("ACCEPTED", "Notification queued")));

        StepVerifier.create(orderService.createOrder(request))
                .assertNext(response -> {
                    assertThat(response.getId()).isEqualTo(1L);
                    assertThat(response.getCustomerId()).isEqualTo("CUST-1");
                    assertThat(response.getDiscountAmount()).isEqualByComparingTo("15.00");
                    assertThat(response.getTotalAmount()).isEqualByComparingTo("85.00");
                    assertThat(response.getItems()).hasSize(1);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Create order falls back to 0% discount when discount client fails")
    void createOrderFallbackWhenDiscountFails() {
        CreateOrderItemRequest itemRequest = new CreateOrderItemRequest("PROD-1", 1, new BigDecimal("100.00"));
        CreateOrderRequest request = new CreateOrderRequest("CUST-1", "BROKENPROMO", List.of(itemRequest));

        Order savedOrder = Order.builder()
                .id(2L)
                .customerId("CUST-1")
                .status(OrderStatus.NEW)
                .totalAmount(new BigDecimal("100.00"))
                .discountAmount(BigDecimal.ZERO)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        OrderItem savedItem = OrderItem.builder()
                .id(20L)
                .orderId(2L)
                .productId("PROD-1")
                .quantity(1)
                .price(new BigDecimal("100.00"))
                .build();

        when(discountClient.getDiscount("BROKENPROMO"))
                .thenReturn(Mono.error(new ExternalServiceException("Service Unavailable")));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(savedOrder));
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(Mono.just(savedItem));
        when(notificationClient.sendNotification(anyLong(), any(), any()))
                .thenReturn(Mono.just(new NotificationResponse("ACCEPTED", "Notification queued")));

        StepVerifier.create(orderService.createOrder(request))
                .assertNext(response -> {
                    assertThat(response.getId()).isEqualTo(2L);
                    assertThat(response.getDiscountAmount()).isEqualByComparingTo("0.00");
                    assertThat(response.getTotalAmount()).isEqualByComparingTo("100.00");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Get order by ID successfully returns OrderResponse")
    void getOrderByIdSuccess() {
        Order order = Order.builder()
                .id(1L)
                .customerId("CUST-1")
                .status(OrderStatus.NEW)
                .totalAmount(new BigDecimal("100.00"))
                .discountAmount(BigDecimal.ZERO)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        OrderItem item = OrderItem.builder()
                .id(10L)
                .orderId(1L)
                .productId("PROD-1")
                .quantity(1)
                .price(new BigDecimal("100.00"))
                .build();

        when(orderRepository.findById(1L)).thenReturn(Mono.just(order));
        when(orderItemRepository.findAllByOrderId(1L)).thenReturn(Flux.just(item));

        StepVerifier.create(orderService.getOrderById(1L))
                .assertNext(response -> {
                    assertThat(response.getId()).isEqualTo(1L);
                    assertThat(response.getCustomerId()).isEqualTo("CUST-1");
                    assertThat(response.getItems()).hasSize(1);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Get order by ID throws OrderNotFoundException when missing")
    void getOrderByIdNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(orderService.getOrderById(99L))
                .expectError(OrderNotFoundException.class)
                .verify();
    }

    @Test
    @DisplayName("Get all orders returns Flux of OrderResponse")
    void getAllOrdersSuccess() {
        Order order1 = Order.builder().id(1L).customerId("CUST-1").totalAmount(new BigDecimal("100.00")).discountAmount(BigDecimal.ZERO).build();
        Order order2 = Order.builder().id(2L).customerId("CUST-2").totalAmount(new BigDecimal("200.00")).discountAmount(BigDecimal.ZERO).build();

        when(orderRepository.findAll()).thenReturn(Flux.just(order1, order2));
        when(orderItemRepository.findAllByOrderId(anyLong())).thenReturn(Flux.empty());

        StepVerifier.create(orderService.getAllOrders())
                .expectNextMatches(resp -> resp.getId().equals(1L))
                .expectNextMatches(resp -> resp.getId().equals(2L))
                .verifyComplete();
    }

    @Test
    @DisplayName("getOrderAnalytics calculates totals in parallel using Mono.zip")
    void getOrderAnalyticsSuccess() {
        Order order1 = Order.builder().id(1L).totalAmount(new BigDecimal("100.00")).discountAmount(new BigDecimal("10.00")).build();
        Order order2 = Order.builder().id(2L).totalAmount(new BigDecimal("200.00")).discountAmount(new BigDecimal("20.00")).build();

        when(orderRepository.count()).thenReturn(Mono.just(2L));
        when(orderRepository.findAll()).thenReturn(Flux.just(order1, order2));

        StepVerifier.create(orderService.getOrderAnalytics())
                .assertNext(analytics -> {
                    assertThat(analytics.getTotalOrders()).isEqualTo(2L);
                    assertThat(analytics.getTotalRevenue()).isEqualByComparingTo("300.00");
                    assertThat(analytics.getAverageOrderValue()).isEqualByComparingTo("150.00");
                    assertThat(analytics.getTotalDiscountsApplied()).isEqualByComparingTo("30.00");
                })
                .verifyComplete();
    }
}
