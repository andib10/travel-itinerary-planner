package com.app.travel_planner.service;

import com.app.travel_planner.entity.PointOfInterest;
import com.app.travel_planner.repository.PointOfInterestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Batch job that embeds every saved POI without an embedding yet. Safe to re-run:
 * only processes rows where embedding IS NULL.
 */
@Service
public class PoiEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(PoiEmbeddingService.class);
    private static final long RATE_LIMIT_DELAY_MS = 250;

    private final PointOfInterestRepository pointOfInterestRepository;
    private final EmbeddingService embeddingService;

    public PoiEmbeddingService(PointOfInterestRepository pointOfInterestRepository, EmbeddingService embeddingService) {
        this.pointOfInterestRepository = pointOfInterestRepository;
        this.embeddingService = embeddingService;
    }

    public int embedCity(String city) {
        // Only IDs with embedding IS NULL.
        List<Long> pendingIds = pointOfInterestRepository.findIdsWithoutEmbedding(city);
        log.info("Embedding {} POIs for {}", pendingIds.size(), city);

        int embeddedCount = 0;
        for (Long id : pendingIds) {
            // Re-fetch the full entity since findIdsWithoutEmbedding only returns IDs.
            PointOfInterest poi = pointOfInterestRepository.findById(id).orElse(null);
            if (poi == null) {
                continue;
            }

            String summary = buildSummary(poi);
            float[] vector = embeddingService.embed(summary); // Gemini API call
            // Written as a text literal via native query since Hibernate doesn't map pgvector's type.
            pointOfInterestRepository.updateEmbedding(id, EmbeddingService.toPgVectorLiteral(vector));
            embeddedCount++;
            sleep(); // stay under Gemini's free-tier rate limit
        }

        log.info("Embedded {} POIs for {}", embeddedCount, city);
        return embeddedCount;
    }

    // Package-private + static so AdminPoiService can reuse the same embedding text format.
    static String buildSummary(PointOfInterest poi) {
        return "%s. Category: %s. %s".formatted(poi.getName(), poi.getCategory(), poi.getDescription());
    }

    private void sleep() {
        try {
            Thread.sleep(RATE_LIMIT_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore interrupt status, don't swallow it
        }
    }
}
