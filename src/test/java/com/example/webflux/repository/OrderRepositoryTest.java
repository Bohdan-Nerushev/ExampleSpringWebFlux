package com.example.webflux.repository;

import com.example.webflux.domain.entity.Order;
import com.example.webflux.domain.enums.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class OrderRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("test_db")
            .withUsername("test_user")
            .withPassword("test_password");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> String.format("r2dbc:postgresql://%s:%d/%s",
                postgres.getHost(),
                postgres.getFirstMappedPort(),
                postgres.getDatabaseName()));
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private OrderRepository orderRepository;

    @Test
    @DisplayName("Save and find order by ID using StepVerifier")
    void saveAndFindOrderById() {
        Order order = new Order();
        order.setCustomerId("CUST-100");
        order.setStatus(OrderStatus.NEW);
        order.setTotalAmount(new BigDecimal("150.00"));
        order.setDiscountAmount(new BigDecimal("0.00"));
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());

        var savedAndRetrieved = orderRepository.save(order)
                .flatMap(saved -> orderRepository.findById(saved.getId()));

        StepVerifier.create(savedAndRetrieved)
                .assertNext(found -> {
                    assertThat(found.getId()).isNotNull();
                    assertThat(found.getCustomerId()).isEqualTo("CUST-100");
                    assertThat(found.getStatus()).isEqualTo(OrderStatus.NEW);
                    assertThat(found.getTotalAmount()).isEqualByComparingTo("150.00");
                })
                .verifyComplete();
    }
}
