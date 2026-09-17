package com.app.travel_planner.service;

import com.app.travel_planner.util.TransientApiRetry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * Thin client for Gemini's chat/generateContent endpoint, used by the itinerary-generation RAG pipeline.
 */
@Service
public class ChatCompletionService {

    private record Part(String text) {
    }

    private record Content(List<Part> parts) {
    }

    private record GenerationConfig(String responseMimeType, Map<String, Object> responseSchema) {
    }

    private record GenerateRequest(List<Content> contents, GenerationConfig generationConfig) {
    }

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public ChatCompletionService(@Value("${GEMINI_API_KEY}") String apiKey,
                                  @Value("${app.llm.chat-model}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .build();
    }

    // Returns the raw JSON text Gemini generated, unparsed. Caller supplies its own responseSchema.
    public String generate(String prompt, Map<String, Object> responseSchema) {
        GenerateRequest request = new GenerateRequest(
                List.of(new Content(List.of(new Part(prompt)))),
                new GenerationConfig("application/json", responseSchema));

        JsonNode response = TransientApiRetry.withRetry("Gemini generateContent", () -> restClient.post()
                .uri(uriBuilder -> uriBuilder.path("/models/{model}:generateContent")
                        .queryParam("key", apiKey)
                        .build(model))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(JsonNode.class));

        return response.path("candidates").path(0).path("content").path("parts").path(0).path("text").asString("");
    }
}
