package com.example.webflux.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderItemRequest {

    @NotBlank(message = "Product ID must not be blank")
    @Size(max = 255, message = "Product ID must not exceed 255 characters")
    private String productId;

    @NotNull(message = "Quantity must not be null")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotNull(message = "Price must not be null")
    @Positive(message = "Price must be greater than zero")
    @Digits(integer = 17, fraction = 2, message = "Price precision must be numeric(19, 2)")
    private BigDecimal price;
}
