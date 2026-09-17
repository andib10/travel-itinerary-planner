package com.app.travel_planner.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers handleUpstreamApiFailure(): maps upstream API errors to a clean 503 response.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleUpstreamApiFailure_returnsClean503_forServerError() {
        HttpServerErrorException ex = HttpServerErrorException.create(
                HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", HttpHeaders.EMPTY,
                "{\"error\":{\"message\":\"This model is currently experiencing high demand.\"}}"
                        .getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);

        ResponseEntity<Map<String, String>> response = handler.handleUpstreamApiFailure(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        // Clean, actionable message - never the raw upstream body.
        assertThat(response.getBody()).containsEntry("error",
                "The AI service is temporarily unavailable. Please try again in a moment.");
    }

    @Test
    void handleUpstreamApiFailure_returns503_forClientErrorToo() {
        // HttpClientErrorException shares the handler via HttpStatusCodeException.
        org.springframework.web.client.HttpClientErrorException ex =
                org.springframework.web.client.HttpClientErrorException.create(
                        HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", HttpHeaders.EMPTY,
                        "{\"error\":{\"message\":\"Quota exceeded\"}}".getBytes(StandardCharsets.UTF_8),
                        StandardCharsets.UTF_8);

        ResponseEntity<Map<String, String>> response = handler.handleUpstreamApiFailure(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).containsEntry("error",
                "The AI service is temporarily unavailable. Please try again in a moment.");
    }
}
