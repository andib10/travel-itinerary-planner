package com.app.travel_planner.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.function.Supplier;

/**
 * Retries a Gemini API call on a transient 429 (rate limit) or 503 (overload); other
 * statuses are never retried. Shared by ChatCompletionService and EmbeddingService.
 */
public final class TransientApiRetry {

    private static final Logger log = LoggerFactory.getLogger(TransientApiRetry.class);
    private static final int MAX_ATTEMPTS = 3;
    private static final long BACKOFF_MS = 2000;

    private TransientApiRetry() {
    }

    /**
     * @param callDescription short label for the log line
     * @param call            the API call, retried in place on a transient failure
     */
    public static <T> T withRetry(String callDescription, Supplier<T> call) {
        return withRetry(callDescription, call, BACKOFF_MS);
    }

    // Package-private so tests can exercise retry/give-up without real sleeps.
    static <T> T withRetry(String callDescription, Supplier<T> call, long backoffMs) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return call.get();
            } catch (HttpStatusCodeException ex) {
                boolean retryable = ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS
                        || ex.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE;
                if (!retryable || attempt == MAX_ATTEMPTS) {
                    throw ex;
                }
                log.warn("{} failed with {} (attempt {}/{}), retrying in {}ms",
                        callDescription, ex.getStatusCode(), attempt, MAX_ATTEMPTS, backoffMs);
                sleep(backoffMs);
            }
        }
        // Unreachable: the loop above always either returns or throws on the last attempt.
        throw new IllegalStateException("Retry loop exited without returning or throwing");
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while retrying an upstream API call", e);
        }
    }
}
