package com.app.travel_planner.service;

import com.app.travel_planner.entity.PointOfInterest;
import com.app.travel_planner.exception.CityNotFoundException;
import com.app.travel_planner.repository.PointOfInterestRepository;
import com.app.travel_planner.util.GeoUtils;
import tools.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * One-time/occasional ingestion job: pulls POIs from OpenTripMap for the demo cities
 * (or any city — see {@link #ingestCity}'s javadoc) and saves them via
 * PointOfInterestRepository. Not called during normal user requests. Always leaves the
 * city fully embedded too, via {@link PoiEmbeddingService} — no separate manual
 * embedding step required.
 */
@Service
public class PoiIngestionService {

    private static final Logger log = LoggerFactory.getLogger(PoiIngestionService.class);

    private static final List<String> DEMO_CITIES = List.of("Rome", "Paris", "Barcelona");
    private static final String KINDS = "historic,museums,cultural"; // general sweep's kinds filter
    private static final int RADIUS_METERS = 10000; // 10km search radius for both radius sweeps
    private static final int LIMIT = 50; // general sweep's max candidates
    private static final String MIN_RATE = "2"; // OpenTripMap popularity threshold, both radius sweeps
    private static final String FOOD_KINDS = "foods"; // food sweep's kinds filter
    private static final int FOOD_TARGET_COUNT = 10; // stop once this many food POIs are actually SAVED
    private static final int FOOD_FETCH_LIMIT = 100; // candidate pool size to draw that 10 from
    // 10km, not 5km: Paris's icons (Eiffel Tower, Sacre-Coeur) are more spread out
    // than Rome's dense historic core and didn't resolve via autosuggest at 5km.
    private static final int LANDMARK_SEARCH_RADIUS_METERS = 10000;
    private static final long RATE_LIMIT_DELAY_MS = 250; // pause between looped OpenTripMap calls
    // 200m, not 150m: two OSM nodes for the same large building/monument (e.g. Paris's
    // Pantheon, ~164m between its two xids) can legitimately sit further apart than
    // 150m allowed for, letting exact duplicates slip through.
    private static final double DUPLICATE_LANDMARK_RADIUS_METERS = 200;
    // Stripped out before comparing names/titles — see significantTokens().
    private static final Set<String> NAME_STOPWORDS = Set.of(
            "the", "of", "and", "il", "lo", "la", "le", "gli", "i", "di", "de", "del", "della", "dei", "delle");
    // Checked first in pickCategory() when the candidate came from the food sweep.
    private static final List<String> FOOD_KIND_PRIORITY = List.of(
            "restaurants", "cafes", "bars", "pubs", "bakeries", "fast_food", "foods");
    // Checked first in pickCategory() when the candidate came from the general/landmark sweep.
    private static final List<String> LANDMARK_KIND_PRIORITY = List.of(
            "historic", "museums", "monuments_and_memorials", "architecture", "religion",
            "archaeology", "palaces", "fortifications", "historic_architecture", "cultural");
    // A place tagged with any of these should essentially never be shown as a food
    // category, even if it also carries an incidental restaurant/cafe tag (e.g. an
    // attached canteen) — see pickCategory().
    private static final Set<String> NEVER_FOOD_KINDS = Set.of(
            "religion", "churches", "cathedrals", "monasteries", "mosques", "synagogues");
    private static final int MIN_SHARED_NAME_TOKENS = 2; // used in extractTitleMatchesName()

    private record CityCenter(double lat, double lon) {
    }

    /**
     * Hardcoded to each city's historic/tourist core, not the population-weighted
     * centroid OpenTripMap's /places/geoname returns — for Rome that centroid sits
     * ~2km east in a POI-dense residential district (Esquilino) and skews the radius
     * sweep toward minor local sites instead of the well-known landmarks.
     */
    private static final Map<String, CityCenter> CITY_CENTERS = Map.of(
            "Rome", new CityCenter(41.8955, 12.4823), // Piazza Venezia
            "Paris", new CityCenter(48.8534, 2.3488), // Notre-Dame / Point Zero
            "Barcelona", new CityCenter(41.3874, 2.1686) // Placa de Catalunya
    );

    /**
     * Must-see landmarks per city that the radius+rate sweep can't reliably surface:
     * OpenTripMap's "rate" popularity score is inconsistent per landmark (Colosseum=7,
     * Trevi Fountain=3, the real Pantheon=2, while some nearby restaurants outrank it),
     * and /places/radius sorts by distance — so a dense historic core buries famous
     * landmarks regardless of the limit. Resolved individually via /places/autosuggest
     * and merged into the same dedupe/save pipeline as the radius sweep.
     * Public: also used by ItineraryGenerationService to force-include these names in
     * retrieval, so vector-similarity ranking alone can't bury a landmark — single
     * source of truth, no separate list to keep in sync.
     */
    // Map.ofEntries, not Map.of — Map.of tops out at 10 key-value pairs, and this list
    // already outgrew that once Amsterdam/Lisbon joined Rome..Milan.
    public static final Map<String, List<String>> CITY_LANDMARKS = Map.ofEntries(
            Map.entry("Rome", List.of("Colosseum", "Roman Forum", "Pantheon", "Trevi Fountain",
                    "Spanish Steps", "Piazza Navona")),
            Map.entry("Paris", List.of("Eiffel Tower", "Louvre Museum", "Notre Dame", "Arc de Triomphe",
                    "Sacré-Cœur", "Musee d'Orsay", "Panthéon")),
            Map.entry("Barcelona", List.of("Sagrada Família", "Park Güell", "Casa Batlló", "La Pedrera",
                    "Cathedral of Santa Eulalia")),
            Map.entry("Florence", List.of("Uffizi Gallery", "Ponte Vecchio", "Palazzo Vecchio",
                    "Piazza della Signoria", "Boboli Gardens")),
            Map.entry("Vienna", List.of("Schönbrunn Palace", "Hofburg Palace", "Vienna State Opera",
                    "Vienna City Hall")),
            Map.entry("Prague", List.of("Charles Bridge", "Prague Castle", "Old Town Square",
                    "Dancing House", "Vyšehrad")),
            Map.entry("Budapest", List.of("Hungarian Parliament Building", "Buda Castle", "Matthias Church",
                    "Széchenyi Chain Bridge", "Heroes' Square", "St. Stephen's Basilica")),
            Map.entry("Madrid", List.of("Royal Palace of Madrid", "Puerta del Sol", "Museo del Prado",
                    "Museo Nacional Centro de Arte Reina Sofía", "El Retiro Park", "The Temple of Debod",
                    "Plaza Mayor")),
            Map.entry("Milan", List.of("Milan Cathedral", "Sforza Castle", "Galleria Vittorio Emanuele II",
                    "Santa Maria delle Grazie", "Teatro alla Scala", "Pinacoteca di Brera", "Navigli")),
            Map.entry("Amsterdam", List.of("Rijksmuseum Amsterdam", "Anne Frank House", "Van Gogh Museum",
                    "Dam Square", "Vondelpark", "Royal Palace", "Westerkerk", "Stedelijk Museum Amsterdam")),
            Map.entry("Bucharest", List.of("Palace of the Parliament", "Stavropoleos Monastery",
                    "University Square", "National Museum of Art of Romania", "Triumphal Arch",
                    "Dimitrie Gusti National Village Museum in Bucharest", "Ateneul Roman")),
            Map.entry("Brasov", List.of("Black Church", "Council House", "White Tower", "Black Tower",
                    "Weavers' Bastion", "Catherine's Gate")),
            Map.entry("London", List.of("Big Ben", "Tower of London", "Westminster Abbey",
                    "St Paul's Cathedral", "British Museum", "Tower Bridge", "Tate Modern",
                    "Natural History Museum")),
            Map.entry("Lisbon", List.of("Lisbon Cathedral", "Santa Justa Lift",
                    "Monastery of the Hieronymites", "Monument to the Discoveries",
                    "National Ceramic Tile Museum", "Rossio"))
    );

    private record Candidate(String xid, String name, boolean preferFoodCategory, boolean isCuratedLandmark) {
    }

    /**
     * Result of one {@link #ingestCity(String)} call: {@code saved} POIs written, all
     * of which are now also guaranteed embedded (see that method's javadoc) —
     * {@code embedded} is however many actually needed a fresh embedding this call,
     * which can be less than {@code saved} if some of the city's POIs were already
     * embedded from an earlier run.
     */
    public record IngestionResult(int saved, int embedded) {
    }

    private final RestClient restClient;
    private final PointOfInterestRepository pointOfInterestRepository;
    private final PoiEmbeddingService poiEmbeddingService;
    private final LandmarkSuggestionService landmarkSuggestionService;
    private final String apiKey;

    public PoiIngestionService(PointOfInterestRepository pointOfInterestRepository,
                                PoiEmbeddingService poiEmbeddingService,
                                LandmarkSuggestionService landmarkSuggestionService,
                                @Value("${OPENTRIPMAP_API_KEY}") String apiKey) {
        this.pointOfInterestRepository = pointOfInterestRepository;
        this.poiEmbeddingService = poiEmbeddingService;
        this.landmarkSuggestionService = landmarkSuggestionService;
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.opentripmap.com/0.1/en")
                .build();
    }

    public List<String> demoCities() {
        return DEMO_CITIES;
    }

    public void ingestAllDemoCities() {
        for (String city : DEMO_CITIES) {
            ingestCity(city);
        }
    }

    /**
     * Runs the full ingestion pipeline for a city (general sweep + food sweep +
     * curated/suggested landmark lookup, all deduped and validated), then always
     * leaves the city fully embedded before returning — folds in what used to be the
     * separate {@code PoiEmbeddingService.embedCity()} call, so there's exactly one
     * entry point and no manual step to remember. Safely re-runnable, same as before:
     * already-saved POIs are skipped via {@code existsByXid}, and {@code embedCity()}
     * only processes POIs still missing an embedding.
     * <p>
     * For a city outside the hardcoded {@code CITY_CENTERS}/{@code CITY_LANDMARKS} map,
     * the landmark lookup falls back to {@link LandmarkSuggestionService} for candidate
     * names instead of skipping landmarks entirely — every suggested name still goes
     * through the exact same real OpenTripMap {@link #fetchLandmark} lookup as a
     * curated one, so actual POI data always comes from verified external data, never
     * the LLM directly. Behavior for already-curated cities is unchanged.
     */
    public IngestionResult ingestCity(String city) {
        CityCenter center = resolveCityCenter(city);
        // Loaded once up front and appended to as we go, so isDuplicateLandmark() can
        // check new candidates against both pre-existing DB rows AND ones saved earlier
        // in this same run.
        List<PointOfInterest> existingInCity = new ArrayList<>(pointOfInterestRepository.findByCity(city));
        Set<String> seenXids = new HashSet<>(); // in-batch dedupe, cheaper than a DB round-trip
        int savedCount = 0;

        // --- 1. General sweep: broad historic/museums/cultural search ---
        List<Candidate> generalCandidates = fetchNearbyPlaces(center.lat(), center.lon(), KINDS, LIMIT, false);
        log.info("Found {} candidate POIs for {} via radius sweep", generalCandidates.size(), city);
        for (Candidate candidate : generalCandidates) {
            if (trySave(candidate, city, seenXids, existingInCity)) {
                savedCount++;
            }
        }

        // --- 2. Food sweep: separate search + limit so food doesn't get crowded out ---
        sleep();
        List<Candidate> foodCandidates = fetchNearbyPlaces(center.lat(), center.lon(), FOOD_KINDS, FOOD_FETCH_LIMIT, true);
        log.info("Found {} candidate food POIs for {} via dedicated food sweep", foodCandidates.size(), city);
        int savedFoodCount = 0;
        for (Candidate candidate : foodCandidates) {
            if (savedFoodCount >= FOOD_TARGET_COUNT) {
                break; // hit the target save count, not just the fetch limit — stop early
            }
            if (trySave(candidate, city, seenXids, existingInCity)) {
                savedFoodCount++;
                savedCount++;
            }
        }
        if (savedFoodCount < FOOD_TARGET_COUNT) {
            // Ran through the whole FOOD_FETCH_LIMIT pool and still came up short.
            log.warn("Only saved {} of the target {} food POIs for {} — ran out of valid candidates",
                    savedFoodCount, FOOD_TARGET_COUNT, city);
        }

        // --- 3. Curated (or, for an uncurated city, LLM-suggested) landmark lookup ---
        List<String> landmarkNames = CITY_LANDMARKS.get(city);
        if (landmarkNames == null) {
            // Not a curated city: ask the LLM for candidate names rather than silently
            // skipping landmarks. Still just names in, resolved through the same
            // fetchLandmark() lookup as every curated entry below — the LLM never
            // supplies coordinates/descriptions/categories directly.
            landmarkNames = landmarkSuggestionService.suggestLandmarks(city);
        }
        for (String landmark : landmarkNames) {
            sleep();
            List<Candidate> landmarkMatches = fetchLandmark(landmark, center.lat(), center.lon());
            if (landmarkMatches.isEmpty()) {
                log.warn("No exact-name match found for landmark '{}' in {}", landmark, city);
            }
            for (Candidate candidate : landmarkMatches) {
                if (trySave(candidate, city, seenXids, existingInCity)) {
                    savedCount++;
                }
            }
        }

        log.info("Saved {} POIs for {} ({} of them food)", savedCount, city, savedFoodCount);

        // Fold embedding into ingestion so the city is always left fully embedded —
        // no separate manual POST /api/admin/embed/{city} call needed.
        int embeddedCount = poiEmbeddingService.embedCity(city);
        return new IngestionResult(savedCount, embeddedCount);
    }

    /**
     * Fetches, maps and saves a single candidate if it passes all quality/dedupe
     * checks. Returns whether it was actually saved, so callers with a target count
     * (e.g. the food sweep) can keep pulling from a larger candidate pool until they
     * hit it, rather than treating the initial fetch limit as a guaranteed save count.
     */
    private boolean trySave(Candidate candidate, String city, Set<String> seenXids, List<PointOfInterest> existingInCity) {
        String xid = candidate.xid();
        String listName = candidate.name();
        if (xid == null || xid.isBlank() || listName.isBlank()) {
            return false; // malformed candidate, nothing to work with
        }
        // seenXids.add() returns false if already present — covers both in-batch
        // duplicates (same xid returned by two different sweeps) and DB duplicates.
        if (!seenXids.add(xid) || pointOfInterestRepository.existsByXid(xid)) {
            return false;
        }

        sleep();
        JsonNode detail = fetchPlaceDetail(xid); // one extra API call per candidate
        PointOfInterest poi = mapToEntity(detail, city, xid, candidate.preferFoodCategory(), candidate.isCuratedLandmark());
        if (poi == null || isDuplicateLandmark(poi, existingInCity)) {
            return false; // failed quality validation, or same real place under a different xid
        }
        pointOfInterestRepository.save(poi);
        existingInCity.add(poi); // so the next candidate in this run can be deduped against it too
        return true;
    }

    private CityCenter resolveCityCenter(String city) {
        CityCenter curated = CITY_CENTERS.get(city);
        if (curated != null) {
            return curated; // hand-picked tourist-core coordinate
        }
        // Fallback for an uncurated city: OpenTripMap's own geocoder. Lower quality —
        // it's a population-weighted centroid, not necessarily the tourist core.
        JsonNode geocoded = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/places/geoname")
                        .queryParam("name", city)
                        .queryParam("apikey", apiKey)
                        .build())
                .retrieve()
                .body(JsonNode.class);
        // A name the geocoder can't resolve returns {"status":"NOT_FOUND","error":"..."} -
        // no lat/lon fields at all. Without this check, JsonNode.path("lat").asDouble() on
        // a missing field silently defaults to 0.0 instead of throwing, resolving to "null
        // island" (0,0) and producing a deceptively successful-looking 0-POI ingest that's
        // indistinguishable from an already-fully-ingested real city. Fail loudly instead.
        if (geocoded == null || !geocoded.hasNonNull("lat") || !geocoded.hasNonNull("lon")) {
            String reason = geocoded != null && geocoded.hasNonNull("error")
                    ? geocoded.path("error").asText()
                    : "no location data returned";
            throw new CityNotFoundException("Could not find a location for \"" + city + "\" (" + reason + ").");
        }
        return new CityCenter(geocoded.path("lat").asDouble(), geocoded.path("lon").asDouble());
    }

    // Shared by both the general sweep (kinds=historic,museums,cultural) and the food
    // sweep (kinds=foods) — only kinds/limit/preferFoodCategory differ between the two
    // calls in ingestCity().
    private List<Candidate> fetchNearbyPlaces(double lat, double lon, String kinds, int limit, boolean preferFoodCategory) {
        JsonNode response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/places/radius")
                        .queryParam("radius", RADIUS_METERS)
                        .queryParam("lon", lon)
                        .queryParam("lat", lat)
                        .queryParam("kinds", kinds)
                        .queryParam("rate", MIN_RATE)
                        .queryParam("limit", limit)
                        // Required: this endpoint returns GeoJSON by default, which
                        // silently breaks the flat-array parsing below.
                        .queryParam("format", "json")
                        .queryParam("apikey", apiKey)
                        .build())
                .retrieve()
                .body(JsonNode.class);

        List<Candidate> candidates = new ArrayList<>();
        if (response != null) {
            for (JsonNode place : response) {
                // Detail (description, kinds, etc.) isn't fetched yet — this is just
                // the lightweight candidate list, full lookup happens in trySave().
                candidates.add(new Candidate(place.path("xid").asString(null), place.path("name").asString(""),
                        preferFoodCategory, false));
            }
        }
        return candidates;
    }

    /**
     * Only keeps exact (case-insensitive) name matches — /places/autosuggest also
     * returns loosely-related fuzzy matches (e.g. searching "Pantheon" also surfaces
     * nearby restaurants named "... al Pantheon"), which would otherwise reintroduce
     * the noise this landmark lookup exists to avoid.
     */
    private List<Candidate> fetchLandmark(String landmarkName, double lat, double lon) {
        JsonNode response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/places/autosuggest")
                        .queryParam("name", landmarkName) // fuzzy name search, unlike /places/radius
                        .queryParam("radius", LANDMARK_SEARCH_RADIUS_METERS)
                        .queryParam("lon", lon)
                        .queryParam("lat", lat)
                        .queryParam("apikey", apiKey)
                        .build())
                .retrieve()
                .body(JsonNode.class);

        List<Candidate> candidates = new ArrayList<>();
        if (response == null) {
            return candidates;
        }
        for (JsonNode feature : response.path("features")) { // autosuggest returns GeoJSON features
            JsonNode properties = feature.path("properties");
            String name = properties.path("name").asString("");
            if (name.equalsIgnoreCase(landmarkName)) { // discard fuzzy near-matches
                // preferFoodCategory=false, isCuratedLandmark=true — see mapToEntity()
                // and pickCategory() for what each flag changes.
                candidates.add(new Candidate(properties.path("xid").asString(null), name, false, true));
            }
        }
        return candidates;
    }

    private JsonNode fetchPlaceDetail(String xid) {
        return restClient.get()
                .uri("/places/xid/{xid}?apikey={apikey}", xid, apiKey)
                .retrieve()
                .body(JsonNode.class);
    }

    /**
     * Returns null (skip) when the place has no name or no Wikipedia extract actually about it —
     * OpenTripMap sometimes links to an unrelated Wikidata entity. Skipped for curated landmarks,
     * since the exact-name match in fetchLandmark() is confidence enough.
     */
    private PointOfInterest mapToEntity(JsonNode detail, String city, String xid, boolean preferFoodCategory,
                                         boolean isCuratedLandmark) {
        String name = detail.path("name").asString("");
        String extractTitle = detail.path("wikipedia_extracts").path("title").asString(null);
        String description = detail.path("wikipedia_extracts").path("text").asString(null);
        // Reject: no name, no Wikipedia text, or (non-curated only) extract doesn't match this place.
        if (name.isBlank() || description == null || description.isBlank()
                || (!isCuratedLandmark && !extractTitleMatchesName(name, extractTitle))) {
            return null;
        }

        PointOfInterest poi = new PointOfInterest();
        poi.setXid(xid);
        poi.setName(name);
        poi.setDescription(description); // raw Wikipedia extract text, used as-is
        poi.setCategory(pickCategory(detail.path("kinds").asString(""), preferFoodCategory));
        poi.setCity(city);
        poi.setLat(detail.path("point").path("lat").asDouble());
        poi.setLng(detail.path("point").path("lon").asDouble());
        // Almost always absent — OpenTripMap's free tier rarely has structured hours data.
        poi.setOpeningHours(detail.has("opening_hours") ? detail.path("opening_hours").toString() : null);
        poi.setSource("opentripmap");
        return poi;
    }

    /**
     * Requires one token set to fully contain the other, plus at least
     * {@code MIN_SHARED_NAME_TOKENS} shared tokens for multi-word names. Single-token names use
     * a prefix check instead, so a Wikipedia disambiguator like "(Roma)" still matches.
     */
    private boolean extractTitleMatchesName(String name, String extractTitle) {
        if (extractTitle == null || extractTitle.isBlank()) {
            return false;
        }
        String titleWithoutLangPrefix = extractTitle.replaceFirst("^[a-z]{2,3}:", ""); // strip "en:"/"it:" etc.
        Set<String> nameTokens = significantTokens(name);
        Set<String> titleTokens = significantTokens(titleWithoutLangPrefix);
        if (nameTokens.isEmpty() || titleTokens.isEmpty()) {
            return false;
        }
        if (nameTokens.size() < 2 || titleTokens.size() < 2) {
            // Single-word case: prefix check so a disambiguator suffix still matches.
            String normalizedName = name.strip().toLowerCase(Locale.ROOT);
            String normalizedTitle = titleWithoutLangPrefix.strip().toLowerCase(Locale.ROOT);
            return normalizedTitle.startsWith(normalizedName) || normalizedName.startsWith(normalizedTitle);
        }
        // Multi-word case: one set must fully contain the other...
        boolean isSubset = nameTokens.containsAll(titleTokens) || titleTokens.containsAll(nameTokens);
        if (!isSubset) {
            return false;
        }
        // ...and share at least 2 words, not just 1.
        Set<String> shared = new HashSet<>(nameTokens);
        shared.retainAll(titleTokens);
        return shared.size() >= MIN_SHARED_NAME_TOKENS;
    }

    /**
     * Picks a recognizable kind over noise buckets like "sport"/"other". Priority order depends on
     * which sweep found the candidate, but {@code NEVER_FOOD_KINDS} always overrides food kinds.
     * Falls back to the first listed kind if nothing matches either priority list.
     */
    private String pickCategory(String kinds, boolean preferFoodCategory) {
        if (kinds == null || kinds.isBlank()) {
            return "unknown";
        }
        List<String> kindList = List.of(kinds.split(","));
        // Churches etc. always win over food kinds, overriding preferFoodCategory.
        boolean neverFood = kindList.stream().anyMatch(NEVER_FOOD_KINDS::contains);
        List<String> firstPriority = (preferFoodCategory && !neverFood) ? FOOD_KIND_PRIORITY : LANDMARK_KIND_PRIORITY;
        List<String> secondPriority = (preferFoodCategory && !neverFood) ? LANDMARK_KIND_PRIORITY : FOOD_KIND_PRIORITY;
        for (String preferred : firstPriority) {
            if (kindList.contains(preferred)) {
                return preferred;
            }
        }
        for (String preferred : secondPriority) {
            if (kindList.contains(preferred)) {
                return preferred;
            }
        }
        return kindList.get(0); // nothing recognizable on either list — genuine data gap
    }

    /**
     * OpenStreetMap sometimes maps the same landmark as multiple distinct elements. Catches these
     * by requiring name overlap, proximity, and a compatible category together — no single check
     * alone is reliable.
     */
    private boolean isDuplicateLandmark(PointOfInterest candidate, List<PointOfInterest> existing) {
        Set<String> candidateTokens = significantTokens(candidate.getName());
        if (candidateTokens.isEmpty()) {
            return false;
        }
        for (PointOfInterest other : existing) {
            // Check category first — cheapest of the three checks.
            if (!categoriesCompatible(candidate.getCategory(), other.getCategory())) {
                continue;
            }
            double distance = GeoUtils.haversineMeters(
                    candidate.getLat(), candidate.getLng(), other.getLat(), other.getLng());
            if (distance > DUPLICATE_LANDMARK_RADIUS_METERS) {
                continue;
            }
            Set<String> otherTokens = significantTokens(other.getName());
            if (otherTokens.isEmpty()) {
                continue;
            }
            // Same token-subset logic as extractTitleMatchesName's multi-word branch.
            if (candidateTokens.containsAll(otherTokens) || otherTokens.containsAll(candidateTokens)) {
                return true;
            }
        }
        return false;
    }

    private boolean categoriesCompatible(String categoryA, String categoryB) {
        if (categoryA.equals(categoryB)) {
            return true;
        }
        // Food categories are treated as interchangeable; everything else must match exactly.
        return FOOD_KIND_PRIORITY.contains(categoryA) && FOOD_KIND_PRIORITY.contains(categoryB);
    }

    // Lowercase, split on non-letter/digit, drop stopwords.
    private Set<String> significantTokens(String name) {
        if (name == null) {
            return Set.of();
        }
        Set<String> tokens = new HashSet<>();
        for (String token : name.toLowerCase(Locale.ROOT).split("[^\\p{L}0-9]+")) {
            if (!token.isBlank() && !NAME_STOPWORDS.contains(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private void sleep() {
        try {
            Thread.sleep(RATE_LIMIT_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore interrupt status, don't swallow it
        }
    }
}