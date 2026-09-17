package com.app.travel_planner.service;

import com.app.travel_planner.dto.AdminPoiResponse;
import com.app.travel_planner.dto.UpdatePoiRequest;
import com.app.travel_planner.entity.PointOfInterest;
import com.app.travel_planner.exception.PoiInUseException;
import com.app.travel_planner.exception.ResourceNotFoundException;
import com.app.travel_planner.repository.PointOfInterestRepository;
import com.app.travel_planner.repository.PointOfInterestRepository.AdminPoiProjection;
import com.app.travel_planner.repository.StopRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * POI listing/edit/delete for the admin surface.
 */
@Service
public class AdminPoiService {

    private static final Logger log = LoggerFactory.getLogger(AdminPoiService.class);

    private final PointOfInterestRepository pointOfInterestRepository;
    private final StopRepository stopRepository;
    private final EmbeddingService embeddingService;

    public AdminPoiService(PointOfInterestRepository pointOfInterestRepository, StopRepository stopRepository,
                            EmbeddingService embeddingService) {
        this.pointOfInterestRepository = pointOfInterestRepository;
        this.stopRepository = stopRepository;
        this.embeddingService = embeddingService;
    }

    // city is optional; blank/null means every POI across every city.
    public List<AdminPoiResponse> listPois(String city) {
        List<AdminPoiProjection> projections = (city == null || city.isBlank())
                ? pointOfInterestRepository.findAllForAdmin()
                : pointOfInterestRepository.findByCityForAdmin(city);
        return projections.stream().map(this::toResponse).toList();
    }

    // Partial update (null field = unchanged). Only touches name/description/category;
    // re-embeds synchronously, but only if a value actually changed.
    @Transactional
    public AdminPoiResponse updatePoi(Long id, UpdatePoiRequest request) {
        PointOfInterest poi = pointOfInterestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Point of interest not found: " + id));

        boolean changed = false;
        if (request.getName() != null && !request.getName().equals(poi.getName())) {
            poi.setName(request.getName());
            changed = true;
        }
        if (request.getDescription() != null && !request.getDescription().equals(poi.getDescription())) {
            poi.setDescription(request.getDescription());
            changed = true;
        }
        if (request.getCategory() != null && !request.getCategory().equals(poi.getCategory())) {
            poi.setCategory(request.getCategory());
            changed = true;
        }
        pointOfInterestRepository.save(poi);
        // Flush so the native queries below (bypassing Hibernate's persistence context) see the update.
        pointOfInterestRepository.flush();

        if (changed) {
            // Re-embed since name/description/category feed the embedding text and nothing else refreshes it.
            float[] vector = embeddingService.embed(PoiEmbeddingService.buildSummary(poi)); // Gemini API call
            pointOfInterestRepository.updateEmbedding(id, EmbeddingService.toPgVectorLiteral(vector));
            log.info("Re-embedded POI {} after edit (name/description/category changed)", id);
        } else {
            log.info("Updated POI {} — no name/description/category change, skipped re-embed", id);
        }

        // Re-fetch via the same projection listPois() uses, so the response reflects the post-update state.
        return pointOfInterestRepository.findByIdForAdmin(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Point of interest not found: " + id));
    }

    // Blocks deletion (409) if any Stop still references this POI.
    public void deletePoi(Long id) {
        PointOfInterest poi = pointOfInterestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Point of interest not found: " + id));

        long stopCount = stopRepository.countByPointOfInterestId(id);
        if (stopCount > 0) {
            throw new PoiInUseException(id, stopCount);
        }
        pointOfInterestRepository.delete(poi);
    }

    private AdminPoiResponse toResponse(AdminPoiProjection p) {
        return new AdminPoiResponse(p.getId(), p.getXid(), p.getName(), p.getCategory(), p.getCity(),
                p.getLat(), p.getLng(), p.getDescription(),
                Boolean.TRUE.equals(p.getHasDescription()),
                Boolean.TRUE.equals(p.getHasEmbedding()),
                Boolean.TRUE.equals(p.getHasOpeningHours()));
    }
}
