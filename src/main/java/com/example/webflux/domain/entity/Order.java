package com.example.webflux.domain.entity;

import com.example.webflux.domain.enums.OrderStatus;
import jakarta.validation.constraints.Digits;
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
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("orders")
public class Order {

    @Id
    private Long id;

    @NotBlank(message = "Customer ID must not be blank")
    @Size(max = 255, message = "Customer ID must not exceed 255 characters")
    @Column("customer_id")
    private String customerId;

    @NotNull(message = "Status must not be null")
    @Column("status")
    private OrderStatus status;

    @NotNull(message = "Total amount must not be null")
    @PositiveOrZero(message = "Total amount must be greater than or equal to zero")
    @Digits(integer = 17, fraction = 2, message = "Total amount precision must be numeric(19, 2)")
    @Column("total_amount")
    private BigDecimal totalAmount;

    @NotNull(message = "Discount amount must not be null")
    @PositiveOrZero(message = "Discount amount must be greater than or equal to zero")
    @Digits(integer = 17, fraction = 2, message = "Discount amount precision must be numeric(19, 2)")
    @Column("discount_amount")
    private BigDecimal discountAmount;

    @NotNull(message = "Created at timestamp must not be null")
    @Column("created_at")
    private Instant createdAt;

    @NotNull(message = "Updated at timestamp must not be null")
    @Column("updated_at")
    private Instant updatedAt;
}
