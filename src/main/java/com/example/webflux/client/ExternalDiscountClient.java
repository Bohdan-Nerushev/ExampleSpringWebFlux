package com.example.webflux.client;

import com.example.webflux.dto.response.DiscountResponse;
import com.example.webflux.exception.ExternalServiceException;

import lombok.AllArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Component
@AllArgsConstructor
public class ExternalDiscountClient {

    private final WebClient webClient;

    public Mono<DiscountResponse> getDiscount(String promoCode) {
        return webClient.get()
                .uri("/{code}", promoCode)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> Mono.error(new ExternalServiceException("Failed to fetch discount for code: " + promoCode)))
                .bodyToMono(DiscountResponse.class)
                .timeout(Duration.ofSeconds(3))
                .retryWhen(Retry.backoff(3, Duration.ofMillis(100))
                        .filter(e -> !(e instanceof ExternalServiceException)))
                .onErrorResume(ExternalServiceException.class, Mono::error)
                .onErrorMap(e -> !(e instanceof ExternalServiceException),
                        e -> new ExternalServiceException("Discount service call failed: " + e.getMessage()));
    }
}
