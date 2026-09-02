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
    @DisplayName("Get order by ID throws OrderNotFoundException when missing")
    void getOrderByIdNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(orderService.getOrderById(99L))
                .expectError(OrderNotFoundException.class)
                .verify();
    }
}
