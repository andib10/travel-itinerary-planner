package com.app.travel_planner.repository;

import com.app.travel_planner.entity.PointOfInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface PointOfInterestRepository extends JpaRepository<PointOfInterest, Long> {
    boolean existsByXid(String xid);

    // Fail-fast check for an unsupported/uningested destination.
    boolean existsByCityIgnoreCase(String city);

    // Powers the unsupported-destination error message's "try one of these" list.
    @Query("SELECT DISTINCT p.city FROM PointOfInterest p ORDER BY p.city")
    List<String> findDistinctCities();

    List<PointOfInterest> findByCity(String city);

    // Add-a-stop picker: case-insensitive since city names aren't normalized elsewhere.
    List<PointOfInterest> findByCityIgnoreCase(String city);

    List<PointOfInterest> findByCityIgnoreCaseAndNameIn(String city, List<String> names);

    @Query(value = "SELECT id FROM points_of_interest WHERE city = :city AND embedding IS NULL", nativeQuery = true)
    List<Long> findIdsWithoutEmbedding(@Param("city") String city);

    // Hibernate can't map the pgvector "vector" column, so write it via native query with a cast.
    @Transactional
    @Modifying
    @Query(value = "UPDATE points_of_interest SET embedding = CAST(:embeddingLiteral AS vector) WHERE id = :id",
            nativeQuery = true)
    void updateEmbedding(@Param("id") Long id, @Param("embeddingLiteral") String embeddingLiteral);

    // Candidate retrieval for the generation pipeline: nearest POIs in the requested city.
    @Query(value = "SELECT * FROM points_of_interest " +
            "WHERE LOWER(city) = LOWER(:city) " +
            "ORDER BY embedding <=> CAST(:queryVectorLiteral AS vector) " +
            "LIMIT :limit",
            nativeQuery = true)
    List<PointOfInterest> findNearestByCity(@Param("city") String city,
                                             @Param("queryVectorLiteral") String queryVectorLiteral,
                                             @Param("limit") int limit);

    // Admin POI listing. Native + interface projection since the entity doesn't map "embedding".
    interface AdminPoiProjection {
        Long getId();

        String getXid();

        String getName();

        String getCategory();

        String getCity();

        Double getLat();

        Double getLng();

        // Full text, so the admin edit dialog can pre-fill it.
        String getDescription();

        Boolean getHasDescription();

        Boolean getHasEmbedding();

        Boolean getHasOpeningHours();
    }

    String ADMIN_POI_SELECT = "SELECT id, xid, name, category, city, lat, lng, description, "
            + "(description IS NOT NULL AND description <> '') AS has_description, "
            + "(embedding IS NOT NULL) AS has_embedding, "
            + "(opening_hours IS NOT NULL AND opening_hours <> '') AS has_opening_hours "
            + "FROM points_of_interest ";

    @Query(value = ADMIN_POI_SELECT + "ORDER BY city, name", nativeQuery = true)
    List<AdminPoiProjection> findAllForAdmin();

    @Query(value = ADMIN_POI_SELECT + "WHERE city = :city ORDER BY name", nativeQuery = true)
    List<AdminPoiProjection> findByCityForAdmin(@Param("city") String city);

    // Single-row version, used to build an accurate post-update response.
    @Query(value = ADMIN_POI_SELECT + "WHERE id = :id", nativeQuery = true)
    Optional<AdminPoiProjection> findByIdForAdmin(@Param("id") Long id);
}
