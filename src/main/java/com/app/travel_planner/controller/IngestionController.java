package com.app.travel_planner.controller;

import com.app.travel_planner.service.PoiEmbeddingService;
import com.app.travel_planner.service.PoiIngestionService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Temporary trigger for the one-time POI ingestion + embedding jobs
 */
@RestController
@RequestMapping("/api/admin")
public class IngestionController {

    private final PoiIngestionService poiIngestionService;
    private final PoiEmbeddingService poiEmbeddingService;

    public IngestionController(PoiIngestionService poiIngestionService, PoiEmbeddingService poiEmbeddingService) {
        this.poiIngestionService = poiIngestionService;
        this.poiEmbeddingService = poiEmbeddingService;
    }

    @PostMapping("/ingest/{city}")
    public Map<String, Object> ingestCity(@PathVariable String city) {
        PoiIngestionService.IngestionResult result = poiIngestionService.ingestCity(city);
        return Map.of("city", city, "saved", result.saved(), "embedded", result.embedded());
    }

    @PostMapping("/ingest")
    public Map<String, Object> ingestAllDemoCities() {
        poiIngestionService.ingestAllDemoCities();
        return Map.of("cities", poiIngestionService.demoCities());
    }

    @PostMapping("/embed/{city}")
    public Map<String, Object> embedCity(@PathVariable String city) {
        int embedded = poiEmbeddingService.embedCity(city);
        return Map.of("city", city, "embedded", embedded);
    }
}