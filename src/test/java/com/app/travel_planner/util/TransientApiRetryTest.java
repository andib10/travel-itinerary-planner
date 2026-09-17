package com.app.travel_planner.util;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Uses backoffMs=0 so retry/give-up behavior is tested without the production delay.
 */
class TransientApiRetryTest {

    private static HttpStatusCodeException serviceUnavailable() {
        return HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable",
                HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8);
    }

    private static HttpStatusCodeException tooManyRequests() {
        return HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests",
                HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8);
    }

    private static HttpStatusCodeException badRequest() {
        return HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request",
                HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8);
    }

    @Test
    void succeedsOnFirstAttempt_neverRetries() {
        AtomicInteger calls = new AtomicInteger();
        String result = TransientApiRetry.withRetry("test call", () -> {
            calls.incrementAndGet();
            return "ok";
        }, 0);

        assertThat(result).isEqualTo("ok");
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void retriesOn503_andSucceedsOnSecondAttempt() {
        AtomicInteger calls = new AtomicInteger();
        String result = TransientApiRetry.withRetry("test call", () -> {
            if (calls.incrementAndGet() == 1) {
                throw serviceUnavailable();
            }
            return "ok";
        }, 0);

        assertThat(result).isEqualTo("ok");
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void retriesOn429_sameAs503() {
        AtomicInteger calls = new AtomicInteger();
        String result = TransientApiRetry.withRetry("test call", () -> {
            if (calls.incrementAndGet() == 1) {
                throw tooManyRequests();
            }
            return "ok";
        }, 0);

        assertThat(result).isEqualTo("ok");
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void givesUpAfterMaxAttempts_stillFailing() {
        AtomicInteger calls = new AtomicInteger();

        assertThatThrownBy(() -> TransientApiRetry.withRetry("test call", () -> {
            calls.incrementAndGet();
            throw serviceUnavailable();
        }, 0)).isInstanceOf(HttpStatusCodeException.class);

        assertThat(calls.get()).isEqualTo(3); // MAX_ATTEMPTS, not retried indefinitely
    }

    @Test
    void doesNotRetry_nonTransientStatus() {
        AtomicInteger calls = new AtomicInteger();

        assertThatThrownBy(() -> TransientApiRetry.withRetry("test call", () -> {
            calls.incrementAndGet();
            throw badRequest();
        }, 0)).isInstanceOf(HttpStatusCodeException.class);

        assertThat(calls.get()).isEqualTo(1); // failed fast, no wasted retries
    }
}
