package com.example.webflux.repository;

import com.example.webflux.domain.entity.OrderItem;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface OrderItemRepository extends R2dbcRepository<OrderItem, Long> {

    Flux<OrderItem> findAllByOrderId(Long orderId);

    Mono<Void> deleteAllByOrderId(Long orderId);
}
