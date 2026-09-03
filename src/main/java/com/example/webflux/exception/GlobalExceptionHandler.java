package com.example.webflux.exception;

import com.example.webflux.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidationException(WebExchangeBindException ex, ServerWebExchange exchange) {
        log.warn("Validation error on path {}: {}", exchange.getRequest().getPath(), ex.getMessage());

        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        ErrorResponse errorResponse = ErrorResponse.builder()
                .path(exchange.getRequest().getPath().value())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed for input payload")
                .details(details)
                .timestamp(Instant.now())
                .build();

        return Mono.just(ResponseEntity.badRequest().body(errorResponse));
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleOrderNotFoundException(OrderNotFoundException ex, ServerWebExchange exchange) {
        log.warn("Resource not found on path {}: {}", exchange.getRequest().getPath(), ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .path(exchange.getRequest().getPath().value())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(ex.getMessage())
                .timestamp(Instant.now())
                .build();

        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse));
    }

    @ExceptionHandler(ExternalServiceException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleExternalServiceException(ExternalServiceException ex, ServerWebExchange exchange) {
        log.error("External service error on path {}: {}", exchange.getRequest().getPath(), ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .path(exchange.getRequest().getPath().value())
                .status(HttpStatus.BAD_GATEWAY.value())
                .error(HttpStatus.BAD_GATEWAY.getReasonPhrase())
                .message(ex.getMessage())
                .timestamp(Instant.now())
                .build();

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorResponse));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGeneralException(Exception ex, ServerWebExchange exchange) {
        log.error("Unexpected error occurred on path {}: {}", exchange.getRequest().getPath(), ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .path(exchange.getRequest().getPath().value())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("An unexpected internal server error occurred")
                .timestamp(Instant.now())
                .build();

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
    }
}
