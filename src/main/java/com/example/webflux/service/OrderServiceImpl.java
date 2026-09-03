package com.example.webflux.service;

import com.example.webflux.client.ExternalDiscountClient;
import com.example.webflux.client.ExternalNotificationClient;
import com.example.webflux.domain.entity.Order;
import com.example.webflux.domain.entity.OrderItem;
import com.example.webflux.domain.enums.OrderStatus;
import com.example.webflux.dto.internal.DiscountCalculation;
import com.example.webflux.dto.request.CreateOrderItemRequest;
import com.example.webflux.dto.request.CreateOrderRequest;
import com.example.webflux.dto.response.DiscountResponse;
import com.example.webflux.dto.response.OrderAnalyticsResponse;
import com.example.webflux.dto.response.OrderItemResponse;
import com.example.webflux.dto.response.OrderResponse;
import com.example.webflux.exception.OrderNotFoundException;
import com.example.webflux.mapper.OrderMapper;
import com.example.webflux.repository.OrderItemRepository;
import com.example.webflux.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ExternalDiscountClient discountClient;
    private final ExternalNotificationClient notificationClient;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public Mono<OrderResponse> createOrder(CreateOrderRequest request) {
        log.info("Creating reactive order for customer: {}", request.getCustomerId());

        BigDecimal rawTotal = request.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Mono<DiscountCalculation> discountMono = getDiscountCalculation(request.getPromoCode(), rawTotal);

        return discountMono.flatMap(calc -> {
            Order newOrder = Order.builder()
                    .customerId(request.getCustomerId())
                    .status(OrderStatus.NEW)
                    .totalAmount(calc.finalTotal())
                    .discountAmount(calc.discountAmount())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            return orderRepository.save(newOrder)
                    .flatMap(savedOrder -> saveOrderItems(savedOrder.getId(), request.getItems())
                            .collectList()
                            .flatMap(savedItems -> {
                                notificationClient.sendNotification(savedOrder.getId(), savedOrder.getCustomerId(), "Order created")
                                        .doOnError(e -> log.warn("Notification error for order {}", savedOrder.getId(), e))
                                        .subscribe();

                                return Mono.just(orderMapper.toOrderResponse(savedOrder, savedItems));
                            }));
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<OrderResponse> getOrderById(Long id) {
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new OrderNotFoundException(id)))
                .flatMap(order -> orderItemRepository.findAllByOrderId(order.getId())
                        .map(orderMapper::toOrderItemResponse)
                        .collectList()
                        .map(items -> orderMapper.toOrderResponse(order, items)));
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<OrderResponse> getAllOrders() {
        return orderRepository.findAll()
                .flatMap(order -> orderItemRepository.findAllByOrderId(order.getId())
                        .map(orderMapper::toOrderItemResponse)
                        .collectList()
                        .map(items -> orderMapper.toOrderResponse(order, items)));
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<OrderAnalyticsResponse> getOrderAnalytics() {
        log.info("Calculating order analytics asynchronously using Mono.zip");

        Mono<Long> totalOrdersMono = orderRepository.count();

        Mono<List<Order>> allOrdersMono = orderRepository.findAll().collectList();

        return Mono.zip(totalOrdersMono, allOrdersMono)
                .map(tuple -> {
                    Long count = tuple.getT1();
                    List<Order> orders = tuple.getT2();

                    if (count == 0 || orders.isEmpty()) {
                        return OrderAnalyticsResponse.builder()
                                .totalOrders(0L)
                                .totalRevenue(BigDecimal.ZERO)
                                .averageOrderValue(BigDecimal.ZERO)
                                .totalDiscountsApplied(BigDecimal.ZERO)
                                .build();
                    }

                    BigDecimal totalRevenue = orders.stream()
                            .map(Order::getTotalAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal totalDiscounts = orders.stream()
                            .map(Order::getDiscountAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal averageOrderValue = totalRevenue.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);

                    return OrderAnalyticsResponse.builder()
                            .totalOrders(count)
                            .totalRevenue(totalRevenue)
                            .averageOrderValue(averageOrderValue)
                            .totalDiscountsApplied(totalDiscounts)
                            .build();
                });
    }

    private Mono<DiscountCalculation> getDiscountCalculation(String promoCode, BigDecimal rawTotal) {
        if (promoCode == null || promoCode.isBlank()) {
            return Mono.just(new DiscountCalculation(BigDecimal.ZERO, rawTotal));
        }

        return discountClient.getDiscount(promoCode)
                .map(discountResponse -> calculateDiscount(discountResponse, rawTotal))
                .onErrorResume(e -> {
                    log.warn("Failed to fetch discount for promoCode: {}, proceeding without discount", promoCode, e);
                    return Mono.just(new DiscountCalculation(BigDecimal.ZERO, rawTotal));
                });
    }

    private DiscountCalculation calculateDiscount(DiscountResponse discountResponse, BigDecimal rawTotal) {
        if (Boolean.TRUE.equals(discountResponse.getValid()) && discountResponse.getPercentage() != null) {
            BigDecimal percentage = discountResponse.getPercentage().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            BigDecimal discountAmount = rawTotal.multiply(percentage).setScale(2, RoundingMode.HALF_UP);
            BigDecimal finalTotal = rawTotal.subtract(discountAmount);
            return new DiscountCalculation(discountAmount, finalTotal);
        }
        return new DiscountCalculation(BigDecimal.ZERO, rawTotal);
    }

    private Flux<OrderItemResponse> saveOrderItems(Long orderId, List<CreateOrderItemRequest> itemRequests) {
        return Flux.fromIterable(itemRequests)
                .map(itemReq -> OrderItem.builder()
                        .orderId(orderId)
                        .productId(itemReq.getProductId())
                        .quantity(itemReq.getQuantity())
                        .price(itemReq.getPrice())
                        .build())
                .flatMap(orderItemRepository::save)
                .map(orderMapper::toOrderItemResponse);
    }
}
