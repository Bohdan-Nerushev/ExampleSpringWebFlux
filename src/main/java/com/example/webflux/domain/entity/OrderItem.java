package com.example.webflux.domain.entity;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("order_items")
public class OrderItem {

    @Id
    private Long id;

    @NotNull(message = "Order ID must not be null")
    @Column("order_id")
    private Long orderId;

    @NotBlank(message = "Product ID must not be blank")
    @Size(max = 255, message = "Product ID must not exceed 255 characters")
    @Column("product_id")
    private String productId;

    @NotNull(message = "Quantity must not be null")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Column("quantity")
    private Integer quantity;

    @NotNull(message = "Price must not be null")
    @PositiveOrZero(message = "Price must be greater than or equal to zero")
    @Digits(integer = 17, fraction = 2, message = "Price precision must be numeric(19, 2)")
    @Column("price")
    private BigDecimal price;
}
