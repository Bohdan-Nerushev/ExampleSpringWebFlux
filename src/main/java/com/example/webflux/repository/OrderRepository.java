package com.example.webflux.repository;

import com.example.webflux.domain.entity.Order;
import com.example.webflux.domain.enums.OrderStatus;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface OrderRepository extends R2dbcRepository<Order, Long> {

    Flux<Order> findByCustomerId(String customerId);

    Flux<Order> findByStatus(OrderStatus status);
}
