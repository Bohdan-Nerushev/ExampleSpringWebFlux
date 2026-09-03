package com.example.webflux.controller;

import com.example.webflux.dto.request.CreateOrderRequest;
import com.example.webflux.dto.response.OrderResponse;
import com.example.webflux.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order Controller", description = "Reactive REST API endpoints for managing orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new order",
            description = "Calculates total amount, applies promo code discount asynchronously, saves order items and triggers notification.")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Order successfully created",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = OrderResponse.class))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request payload or validation error",
                            content = @Content)})
    public Mono<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("REST request to create order for customer: {}", request.getCustomerId());
        return orderService.createOrder(request)
                .doOnSuccess(order -> log.info("Successfully created order with ID: {}", order.getId()))
                .doOnError(e -> log.error("Failed to create order for customer: {}", request.getCustomerId(), e));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get order by ID",
            description = "Fetches a specific order by its unique identifier.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Order found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Order not found",
                    content = @Content)})
    public Mono<ResponseEntity<OrderResponse>> getOrderById(@Parameter(description = "Unique ID of the order", example = "1") @PathVariable Long id) {
        log.debug("REST request to fetch order by ID: {}", id);
        return orderService.getOrderById(id).map(ResponseEntity::ok)
                .doOnSuccess(response -> log.debug("Found order with ID: {}", id))
                .doOnError(e -> log.warn("Order not found with ID: {}", id));
    }

    @GetMapping
    @Operation(
            summary = "Get all orders",
            description = "Returns a reactive Flux list of all orders.")
    @ApiResponse(
            responseCode = "200",
            description = "List of orders",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = OrderResponse.class))))
    public Flux<OrderResponse> getAllOrders() {
        log.debug("REST request to fetch all orders");
        return orderService.getAllOrders()
                .doOnComplete(() -> log.debug("Completed fetching all orders"));
    }

    @GetMapping(
            value = "/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Stream all orders (SSE)",
            description = "Streams orders in real-time using Server-Sent Events (SSE).")
    @ApiResponse(
            responseCode = "200",
            description = "Server-Sent Events stream of orders",
            content = @Content(
                    mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
                    array = @ArraySchema(
                            schema = @Schema(implementation = OrderResponse.class))))
    public Flux<OrderResponse> streamAllOrders() {
        log.debug("REST request to stream all orders via SSE");
        return orderService.getAllOrders()
                .doOnComplete(() -> log.debug("Completed streaming all orders via SSE"));
    }
}
