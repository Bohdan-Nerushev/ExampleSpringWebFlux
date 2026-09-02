package com.example.webflux.mapper;

import com.example.webflux.domain.entity.Order;
import com.example.webflux.domain.entity.OrderItem;
import com.example.webflux.dto.response.OrderItemResponse;
import com.example.webflux.dto.response.OrderResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderMapper {

    public OrderItemResponse toOrderItemResponse(OrderItem item) {
        if (item == null) {
            return null;
        }
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .build();
    }

    public OrderResponse toOrderResponse(Order order, List<OrderItemResponse> items) {
        if (order == null) {
            return null;
        }
        return OrderResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .items(items)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
