package com.app.travel_planner.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Suggests must-see landmark names for a city outside the hardcoded {@code CITY_LANDMARKS} list.
 * The LLM only supplies names, never coordinates/descriptions — each name is still resolved via
 * a real OpenTripMap lookup, so a hallucination just fails to resolve rather than entering the dataset.
 * Resolved names are cached in memory (not persisted) so {@code ItineraryGenerationService} can reuse
 * them without a new LLM call; lost on restart, an accepted tradeoff.
 */
@Service
public class LandmarkSuggestionService {

    private static final Logger log = LoggerFactory.getLogger(LandmarkSuggestionService.class);
    private static final int SUGGESTION_COUNT = 8;
    private static final Map<String, Object> LANDMARK_RESPONSE_SCHEMA = Map.of(
            "type", "ARRAY",
            "items", Map.of("type", "STRING"));

    private final ChatCompletionService chatCompletionService;
    private final ObjectMapper objectMapper;
    private final Map<String, List<String>> knownLandmarksByCity = new ConcurrentHashMap<>();

    public LandmarkSuggestionService(ChatCompletionService chatCompletionService, ObjectMapper objectMapper) {
        this.chatCompletionService = chatCompletionService;
        this.objectMapper = objectMapper;
    }

    /**
     * @return up to {@value SUGGESTION_COUNT} landmark names, or empty if the call/parse fails.
     */
    public List<String> suggestLandmarks(String city) {
        String prompt = ("List the %d most well-known must-see tourist landmarks in %s, by their common "
                + "English name. Respond with ONLY a JSON array of strings, no markdown fences, no extra "
                + "commentary — e.g. [\"Landmark One\", \"Landmark Two\"]. Do not include coordinates, "
                + "descriptions, or categories, just the plain names.").formatted(SUGGESTION_COUNT, city);

        String rawJson = chatCompletionService.generate(prompt, LANDMARK_RESPONSE_SCHEMA); // Gemini API call
        try {
            List<String> landmarks = List.of(objectMapper.readValue(rawJson, String[].class));
            log.info("Suggested {} landmark name(s) for uncurated city '{}': {}", landmarks.size(), city, landmarks);
            knownLandmarksByCity.put(city, landmarks);
            return landmarks;
        } catch (Exception e) {
            log.warn("Failed to parse landmark suggestions for '{}', continuing without guaranteed landmarks: {}",
                    city, e.getMessage());
            return List.of();
        }
    }

    /**
     * @return the names last suggested for {@code city}, without a new LLM call; empty if none cached.
     */
    public List<String> getKnownLandmarks(String city) {
        return knownLandmarksByCity.getOrDefault(city, List.of());
    }
}
