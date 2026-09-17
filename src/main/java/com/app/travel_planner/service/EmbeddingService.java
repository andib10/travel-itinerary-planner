package com.app.travel_planner.service;

import com.app.travel_planner.util.TransientApiRetry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.util.List;

/**
 * Thin client for the Gemini embeddings endpoint, shared by POI ingestion and trip-request embedding.
 */
@Service
public class EmbeddingService {

    private static final String MODEL = "gemini-embedding-001";
    private static final int OUTPUT_DIMENSIONALITY = 768;

    private record Part(String text) {
    }

    private record Content(List<Part> parts) {
    }

    private record EmbedRequest(Content content, int outputDimensionality) {
    }

    private final RestClient restClient;
    private final String apiKey;

    public EmbeddingService(@Value("${GEMINI_API_KEY}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .build();
    }

    public float[] embed(String text) {
        EmbedRequest request = new EmbedRequest(new Content(List.of(new Part(text))), OUTPUT_DIMENSIONALITY);

        JsonNode response = TransientApiRetry.withRetry("Gemini embedContent", () -> restClient.post()
                .uri(uriBuilder -> uriBuilder.path("/models/{model}:embedContent")
                        .queryParam("key", apiKey)
                        .build(MODEL))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(JsonNode.class));

        JsonNode values = response.path("embedding").path("values");
        float[] vector = new float[values.size()];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = (float) values.get(i).asDouble();
        }
        return vector;
    }

    // pgvector's text input format: "[v1,v2,v3,...]"
    public static String toPgVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        return sb.append(']').toString();
    }
}
