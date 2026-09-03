package com.example.webflux.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CreateOrderRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("Should pass validation when all fields are valid")
    void validCreateOrderRequest() {
        CreateOrderItemRequest item = new CreateOrderItemRequest("PROD-100", 2, new BigDecimal("49.99"));
        CreateOrderRequest request = new CreateOrderRequest("CUST-1", "SUMMER2026", List.of(item));

        Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when customerId is blank or promoCode/items are invalid")
    void invalidCreateOrderRequest() {
        CreateOrderItemRequest invalidItem = new CreateOrderItemRequest("", 0, new BigDecimal("-10.00"));
        CreateOrderRequest request = new CreateOrderRequest("", "PROMO", List.of(invalidItem));

        Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(4);
    }

    @Test
    @DisplayName("Should fail validation when items list is empty")
    void emptyItemsList() {
        CreateOrderRequest request = new CreateOrderRequest("CUST-1", null, Collections.emptyList());

        Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Order must contain at least one item");
    }
}
