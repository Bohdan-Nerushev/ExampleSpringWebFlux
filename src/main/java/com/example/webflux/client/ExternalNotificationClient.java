package com.example.webflux.client;

import com.example.webflux.dto.response.NotificationResponse;
import com.example.webflux.exception.ExternalServiceException;

import lombok.AllArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@AllArgsConstructor
public class ExternalNotificationClient {

    private final WebClient webClient;

    public Mono<NotificationResponse> sendNotification(Long orderId, String customerId, String message) {
        Map<String, Object> payload = Map.of(
                "orderId", orderId,
                "customerId", customerId,
                "message", message
        );

        return webClient.post()
                .bodyValue(payload)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> Mono.error(new ExternalServiceException("Failed to send notification for order: " + orderId)))
                .bodyToMono(NotificationResponse.class)
                .onErrorResume(ExternalServiceException.class, Mono::error)
                .onErrorMap(e -> !(e instanceof ExternalServiceException),
                        e -> new ExternalServiceException("Notification service call failed: " + e.getMessage()));
    }
}
