package com.example.webflux;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.blockhound.BlockHound;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;

class BlockHoundTest {

    @BeforeAll
    static void setUp() {
        BlockHound.install();
    }

    @Test
    @DisplayName("BlockHound should throw Error when blocking call is detected in non-blocking thread")
    void blockHoundDetectsBlockingCall() {
        Mono<String> blockingMono = Mono.just("test")
                .subscribeOn(Schedulers.parallel())
                .map(val -> {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return val;
                });

        StepVerifier.create(blockingMono)
                .expectError(Error.class)
                .verify(Duration.ofSeconds(5));
    }
}
