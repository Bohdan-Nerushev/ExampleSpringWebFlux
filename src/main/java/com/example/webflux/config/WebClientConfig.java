package com.example.webflux.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Value("${integration.wiremock.discount-url}")
    private String discountBaseUrl;

    @Value("${integration.wiremock.notification-url}")
    private String notificationBaseUrl;

    @Bean
    public HttpClient httpClient() {
        return HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(5))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(5, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(5, TimeUnit.SECONDS)));
    }

    @Bean
    public WebClient.Builder webClientBuilder(HttpClient httpClient) {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }

    @Bean
    @Primary
    public WebClient discountWebClient(WebClient.Builder builder) {
        return builder.clone()
                .baseUrl(discountBaseUrl)
                .build();
    }

    @Bean
    public WebClient notificationWebClient(WebClient.Builder builder) {
        return builder.clone()
                .baseUrl(notificationBaseUrl)
                .build();
    }
}
