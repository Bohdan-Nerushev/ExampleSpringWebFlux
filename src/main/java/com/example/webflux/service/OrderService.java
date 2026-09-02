package com.example.webflux.service;

import com.example.webflux.dto.request.CreateOrderRequest;
import com.example.webflux.dto.response.OrderResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrderService {

    Mono<OrderResponse> createOrder(CreateOrderRequest request);

    Mono<OrderResponse> getOrderById(Long id);

    Flux<OrderResponse> getAllOrders();
}
