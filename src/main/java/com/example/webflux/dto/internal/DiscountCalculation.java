package com.example.webflux.dto.internal;

import java.math.BigDecimal;

public record DiscountCalculation(
    BigDecimal discountAmount, 
    BigDecimal finalTotal
) {}
