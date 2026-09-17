package com.app.travel_planner.service;

import com.app.travel_planner.dto.CreateDayRequest;
import com.app.travel_planner.dto.CreateStopRequest;
import com.app.travel_planner.dto.CreateTripRequest;
import com.app.travel_planner.dto.GenerateTripRequest;
import com.app.travel_planner.dto.GeneratedDayDraft;
import com.app.travel_planner.dto.GeneratedItineraryDraft;
import com.app.travel_planner.dto.GeneratedStopDraft;
import com.app.travel_planner.dto.ItineraryDayResponse;
import com.app.travel_planner.dto.TripDetailResponse;
import com.app.travel_planner.dto.TripSummaryResponse;
import com.app.travel_planner.entity.PointOfInterest;
import com.app.travel_planner.entity.Stop;
import com.app.travel_planner.entity.User;
import com.app.travel_planner.exception.ItineraryGenerationException;
import com.app.travel_planner.exception.UnsupportedDestinationException;
import com.app.travel_planner.optimization.DayOptimizer;
import com.app.travel_planner.optimization.OptimizationResult;
import com.app.travel_planner.repository.PointOfInterestRepository;
import com.app.travel_planner.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * RAG retrieval + generation pipeline for automatic trip creation: embed the request,
 * retrieve candidate POIs, build the prompt, call the LLM for generation, parse/validate
 * with one repair retry, map + save.
 */
@Service
public class ItineraryGenerationService {

    private static final Logger log = LoggerFactory.getLogger(ItineraryGenerationService.class);
    private static final int CANDIDATE_LIMIT = 25;
    private static final int DESCRIPTION_SNIPPET_LENGTH = 200;
    private static final String ADMIN_ROLE = "ADMIN";

    private final EmbeddingService embeddingService;
    private final PointOfInterestRepository pointOfInterestRepository;
    private final ChatCompletionService chatCompletionService;
    private final ObjectMapper objectMapper;
    private final TripService tripService;
    private final LandmarkSuggestionService landmarkSuggestionService;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public ItineraryGenerationService(EmbeddingService embeddingService,
                                       PointOfInterestRepository pointOfInterestRepository,
                                       ChatCompletionService chatCompletionService,
                                       ObjectMapper objectMapper,
                                       TripService tripService,
                                       LandmarkSuggestionService landmarkSuggestionService,
                                       UserRepository userRepository,
                                       EmailService emailService) {
        this.embeddingService = embeddingService;
        this.pointOfInterestRepository = pointOfInterestRepository;
        this.chatCompletionService = chatCompletionService;
        this.tripService = tripService;
        this.objectMapper = objectMapper;
        this.landmarkSuggestionService = landmarkSuggestionService;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    public float[] embedRequest(GenerateTripRequest request) {
        String summary = buildRequestSummary(request);

        long start = System.currentTimeMillis();
        float[] vector = embeddingService.embed(summary); // Gemini API call
        long elapsedMs = System.currentTimeMillis() - start;

        log.info("Embedded trip request in {}ms (dim={}): \"{}\"", elapsedMs, vector.length, summary);
        return vector;
    }

    // Nearest neighbors by pgvector distance, plus curated landmarks force-included even if
    // similarity alone would rank them outside the top N.
    public List<PointOfInterest> retrieveCandidatePois(String city, float[] queryVector) {
        List<PointOfInterest> nearest = retrieveCandidatePois(city, queryVector, CANDIDATE_LIMIT);
        return withGuaranteedLandmarks(city, nearest);
    }

    // Overload for debugging/tuning: lets the debug controller pull a wider ranking than usual.
    public List<PointOfInterest> retrieveCandidatePois(String city, float[] queryVector, int limit) {
        String queryVectorLiteral = EmbeddingService.toPgVectorLiteral(queryVector);

        long start = System.currentTimeMillis();
        List<PointOfInterest> candidates = pointOfInterestRepository.findNearestByCity(city, queryVectorLiteral, limit);
        long elapsedMs = System.currentTimeMillis() - start;

        log.info("Retrieved {} candidate POIs for {} in {}ms", candidates.size(), city, elapsedMs);
        return candidates;
    }

    // Curated CITY_LANDMARKS merged with any names LandmarkSuggestionService resolved for this city.
    private List<String> getLandmarkNames(String city) {
        List<String> curated = PoiIngestionService.CITY_LANDMARKS.getOrDefault(city, List.of());
        List<String> suggested = landmarkSuggestionService.getKnownLandmarks(city);
        if (suggested.isEmpty()) {
            return curated;
        }
        Set<String> merged = new LinkedHashSet<>(curated);
        merged.addAll(suggested);
        return new ArrayList<>(merged);
    }

    // Appends any guaranteed landmark missing from the vector-similarity result, deduped by id.
    private List<PointOfInterest> withGuaranteedLandmarks(String city, List<PointOfInterest> candidates) {
        List<String> landmarkNames = getLandmarkNames(city);
        if (landmarkNames.isEmpty()) {
            return candidates;
        }

        Set<Long> presentIds = candidates.stream().map(PointOfInterest::getId).collect(Collectors.toSet());
        List<PointOfInterest> landmarks = pointOfInterestRepository.findByCityIgnoreCaseAndNameIn(city, landmarkNames);

        List<PointOfInterest> merged = new ArrayList<>(candidates);
        int addedCount = 0;
        for (PointOfInterest landmark : landmarks) {
            if (presentIds.add(landmark.getId())) {
                merged.add(landmark);
                addedCount++;
            }
        }

        if (addedCount > 0) {
            log.info("Force-included {} curated landmark(s) for {} missing from the vector-similarity top {}",
                    addedCount, city, candidates.size());
        }
        return merged;
    }

    // Builds the generation prompt: candidate POIs + constraints + required JSON shape.
    public String buildPrompt(GenerateTripRequest request, List<PointOfInterest> candidates) {
        long dayCount = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;

        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a travel itinerary planner. Build a day-by-day itinerary for a trip to ")
                .append(request.getDestination())
                .append(" using ONLY the candidate points of interest listed below — do not invent, rename, ")
                .append("or substitute any place. Reference each chosen stop by its exact numeric \"id\" ")
                .append("from the candidate list.\n\n");

        prompt.append("Trip details:\n");
        prompt.append("- Destination: ").append(request.getDestination()).append("\n");
        prompt.append("- Dates: ").append(request.getStartDate()).append(" to ").append(request.getEndDate())
                .append(" (").append(dayCount).append(dayCount == 1 ? " day)\n" : " days)\n");
        prompt.append("- Interests: ").append(String.join(", ", request.getInterests())).append("\n");
        if (!request.getAvoid().isEmpty()) {
            prompt.append("- Avoid: ").append(String.join(", ", request.getAvoid())).append("\n");
        }
        if (request.getPace() != null && !request.getPace().isBlank()) {
            prompt.append("- Pace: ").append(request.getPace()).append("\n");
        }
        prompt.append("\n");

        // Tag landmarks inline so the model can distinguish them from ordinary candidates.
        Set<String> landmarkNames = new HashSet<>(getLandmarkNames(extractCityName(request.getDestination())));
        prompt.append("Candidate points of interest (choose only from these, by id):\n");
        for (PointOfInterest poi : candidates) {
            boolean isLandmark = landmarkNames.contains(poi.getName());
            prompt.append("- id ").append(poi.getId())
                    .append(": ").append(poi.getName())
                    .append(" (").append(poi.getCategory()).append(")")
                    .append(isLandmark ? " [MUST-SEE LANDMARK]" : "")
                    .append(" — ")
                    .append(truncate(poi.getDescription(), DESCRIPTION_SNIPPET_LENGTH))
                    .append("\n");
        }
        prompt.append("\n");

        prompt.append("Rules:\n");
        prompt.append("- Plan exactly ").append(dayCount).append(" day(s), with \"dayNumber\" from 1 to ")
                .append(dayCount).append(".\n");
        prompt.append("- Every day must have at least one stop — never return an empty \"stops\" array.\n");
        prompt.append("- Every \"poiId\" must be one of the candidate ids listed above, as a string — never invent an id or a place.\n");
        prompt.append("- Each stop needs \"startTime\" and \"endTime\" in 24-hour \"HH:mm\" format, with startTime before endTime.\n");
        prompt.append("- Order stops within a day chronologically, leaving sensible travel time between stops, ")
                .append("and pace the number of stops per day to match the requested pace.\n");
        prompt.append("- Distribute stops evenly across the day given the pace — avoid leaving a large, ")
                .append("unexplained idle gap (more than a couple of hours) between two stops, and avoid ")
                .append("cramming the rest of the day's stops too tightly together afterward.\n");
        prompt.append("- Prefer including [MUST-SEE LANDMARK] candidates when they fit the requested pace and ")
                .append("don't conflict with the avoid-list.\n");
        prompt.append("- Respect the avoid-list: don't schedule anything matching it.\n");
        prompt.append("- \"notes\" should be a short reason or tip, one sentence.\n");
        prompt.append("- Don't reuse the same poiId twice across the itinerary.\n\n");

        // poiId "17" is a string here since buildResponseSchema() constrains poiId to STRING.
        prompt.append("Respond with ONLY valid JSON matching this exact shape — no markdown fences, no extra commentary:\n");
        prompt.append("""
                {
                  "days": [
                    {
                      "dayNumber": 1,
                      "stops": [
                        { "poiId": "17", "startTime": "09:00", "endTime": "11:00", "notes": "short reason or tip" }
                      ]
                    }
                  ]
                }
                """);

        return prompt.toString();
    }

    // Built per-request from the candidate list; poiId is constrained via Gemini's STRING-only
    // "enum" support to the exact candidate ids, since an INTEGER field can't be enum-constrained.
    private Map<String, Object> buildResponseSchema(List<PointOfInterest> candidates) {
        List<String> candidateIdStrings = candidates.stream()
                .map(poi -> String.valueOf(poi.getId()))
                .toList();

        return Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "days", Map.of(
                                "type", "ARRAY",
                                "items", Map.of(
                                        "type", "OBJECT",
                                        "properties", Map.of(
                                                "dayNumber", Map.of("type", "INTEGER"),
                                                "stops", Map.of(
                                                        "type", "ARRAY",
                                                        "items", Map.of(
                                                                "type", "OBJECT",
                                                                "properties", Map.of(
                                                                        "poiId", Map.of(
                                                                                "type", "STRING",
                                                                                "enum", candidateIdStrings),
                                                                        "startTime", Map.of("type", "STRING"),
                                                                        "endTime", Map.of("type", "STRING"),
                                                                        "notes", Map.of("type", "STRING")),
                                                                "required", List.of("poiId", "startTime", "endTime")))),
                                        "required", List.of("dayNumber", "stops")))),
                "required", List.of("days"));
    }

    // Calls the LLM to generate itinerary JSON, constrained by the given response schema.
    public String generateItineraryJson(String prompt, Map<String, Object> responseSchema) {
        long start = System.currentTimeMillis();
        String rawJson = chatCompletionService.generate(prompt, responseSchema); // Gemini API call
        long elapsedMs = System.currentTimeMillis() - start;

        log.info("Generated itinerary JSON in {}ms ({} chars)", elapsedMs, rawJson.length());
        return rawJson;
    }

    // Generate, validate, and repair once if needed; throws if still invalid after the retry.
    public GeneratedItineraryDraft generateValidatedDraft(GenerateTripRequest request, List<PointOfInterest> candidates) {
        String prompt = buildPrompt(request, candidates);
        Map<String, Object> responseSchema = buildResponseSchema(candidates);
        String rawJson = generateItineraryJson(prompt, responseSchema);

        ValidationResult result = timedParseAndValidate(rawJson, request, candidates);
        if (result.errors().isEmpty()) {
            log.info("Itinerary JSON passed validation on first try");
            return result.draft();
        }

        log.warn("Itinerary JSON failed validation, attempting one repair retry: {}", result.errors());
        String repairPrompt = buildRepairPrompt(prompt, rawJson, result.errors());
        String repairedJson = generateItineraryJson(repairPrompt, responseSchema);
        ValidationResult repairedResult = timedParseAndValidate(repairedJson, request, candidates);

        if (repairedResult.errors().isEmpty()) {
            log.info("Itinerary JSON passed validation after repair retry");
            return repairedResult.draft();
        }

        log.error("Itinerary JSON failed validation even after repair retry: {}", repairedResult.errors());
        throw new ItineraryGenerationException(repairedResult.errors());
    }

    // Maps the validated draft into real Trip/Day/Stop entities via the normal Trip CRUD
    // methods, running each day through DayOptimizer before saving. @Transactional so a
    // failed save rolls back the whole trip instead of leaving a half-built one.
    @Transactional
    public TripDetailResponse saveGeneratedTrip(GenerateTripRequest request, GeneratedItineraryDraft draft,
                                                 List<PointOfInterest> candidates, User currentUser) {
        long start = System.currentTimeMillis();
        CreateTripRequest createTripRequest = new CreateTripRequest();
        createTripRequest.setDestination(request.getDestination());
        createTripRequest.setStartDate(request.getStartDate());
        createTripRequest.setEndDate(request.getEndDate());
        TripSummaryResponse trip = tripService.createTrip(createTripRequest, currentUser);

        // Validation already confirmed every draft poiId is one of these candidates.
        Map<Long, PointOfInterest> poiById = candidates.stream()
                .collect(Collectors.toMap(PointOfInterest::getId, poi -> poi, (a, b) -> a));

        for (GeneratedDayDraft dayDraft : draft.days()) {
            CreateDayRequest createDayRequest = new CreateDayRequest();
            createDayRequest.setDayNumber(dayDraft.dayNumber());
            createDayRequest.setDate(request.getStartDate().plusDays(dayDraft.dayNumber() - 1));
            ItineraryDayResponse day = tripService.addDay(trip.getId(), createDayRequest, currentUser);

            // Sort by startTime first: RouteSequencer treats index 0 as the fixed day-start anchor.
            List<Stop> dayStops = dayDraft.stops().stream()
                    .sorted(Comparator.comparing(GeneratedStopDraft::startTime))
                    .map(stopDraft -> toTransientStop(stopDraft, poiById))
                    .collect(Collectors.toCollection(ArrayList::new));

            // Resequence geographically then resolve conflicts; mutates dayStops in place.
            OptimizationResult optimizationResult = DayOptimizer.optimizeDay(dayStops);
            logOptimizationResult(trip.getId(), dayDraft.dayNumber(), optimizationResult);

            List<Stop> orderedStops = dayStops.stream()
                    .sorted(Comparator.comparingInt(Stop::getOrderIndex))
                    .toList();

            int orderIndex = 1;
            for (Stop stop : orderedStops) {
                CreateStopRequest createStopRequest = new CreateStopRequest();
                createStopRequest.setPoiId(stop.getPointOfInterest().getId());
                createStopRequest.setStartTime(stop.getStartTime());
                createStopRequest.setEndTime(stop.getEndTime());
                createStopRequest.setNotes(stop.getNotes());
                createStopRequest.setOrderIndex(orderIndex++);
                tripService.addStop(day.getId(), createStopRequest, currentUser);
            }
        }

        tripService.markGenerated(trip.getId(), currentUser);
        long elapsedMs = System.currentTimeMillis() - start;
        log.info("Saved generated trip {} for {} ({} days) in {}ms", trip.getId(), request.getDestination(), draft.days().size(), elapsedMs);
        return tripService.getTripDetail(trip.getId(), currentUser);
    }

    // An unpersisted draft stop, just enough for DayOptimizer to compute travel time/conflicts.
    private Stop toTransientStop(GeneratedStopDraft stopDraft, Map<Long, PointOfInterest> poiById) {
        Stop stop = new Stop();
        // parseLong is safe unguarded: this only runs on a draft that already passed validation.
        stop.setPointOfInterest(poiById.get(Long.parseLong(stopDraft.poiId())));
        stop.setStartTime(LocalTime.parse(stopDraft.startTime()));
        stop.setEndTime(LocalTime.parse(stopDraft.endTime()));
        stop.setNotes(stopDraft.notes());
        return stop;
    }

    private void logOptimizationResult(Long tripId, Integer dayNumber, OptimizationResult result) {
        log.info("Optimized trip {} day {}: {} conflict(s) before -> {} after ({} resolved, {} dropped, {} unresolved), stopsReordered={}",
                tripId, dayNumber, result.conflictsBefore(), result.conflictsAfter(),
                result.resolvedCount(), result.droppedCount(), result.unresolvedCount(), result.stopsReordered());
        result.details().forEach(detail -> log.info("  - {}", detail));
    }

    // Full pipeline behind POST /api/trips/generate: embed, retrieve, generate/validate/repair, save.
    public TripDetailResponse generateTrip(GenerateTripRequest request, User currentUser) {
        long start = System.currentTimeMillis();

        // Fail fast before any LLM call if the destination has zero ingested POIs.
        String city = extractCityName(request.getDestination());
        if (!pointOfInterestRepository.existsByCityIgnoreCase(city)) {
            List<String> supportedCities = pointOfInterestRepository.findDistinctCities();
            log.warn("Destination \"{}\" (normalized city \"{}\") has no ingested POI data - " +
                    "rejecting before any LLM call", request.getDestination(), city);
            // Fire-and-forget so the user's 404 isn't delayed by the SMTP round-trip.
            CompletableFuture.runAsync(() -> notifyAdminsOfMissingCity(request.getDestination(), city, currentUser));
            throw new UnsupportedDestinationException(
                    "We don't have any point-of-interest data for \"" + request.getDestination()
                            + "\" yet, but we've let our team know so it can be added soon. "
                            + "In the meantime, try one of: " + String.join(", ", supportedCities) + ".");
        }

        float[] queryVector = embedRequest(request);
        List<PointOfInterest> candidates = retrieveCandidatePois(city, queryVector);
        GeneratedItineraryDraft draft = generateValidatedDraft(request, candidates);
        TripDetailResponse trip = saveGeneratedTrip(request, draft, candidates, currentUser);

        long elapsedMs = System.currentTimeMillis() - start;
        log.info("End-to-end generation for trip {} ({}) completed in {}ms", trip.getId(), request.getDestination(), elapsedMs);
        return trip;
    }

    // Best-effort admin notification; runs on a background thread and swallows every failure.
    private void notifyAdminsOfMissingCity(String rawDestination, String normalizedCity, User requestedBy) {
        try {
            List<User> admins = userRepository.findByRole(ADMIN_ROLE);
            if (admins.isEmpty()) {
                log.warn("No admin users to notify about missing city \"{}\"", normalizedCity);
                return;
            }
            String subject = "New destination requested: " + normalizedCity;
            String body = "A user requested a trip to \"" + rawDestination + "\" (normalized city: \""
                    + normalizedCity + "\"), which has no ingested point-of-interest data yet.\n\n"
                    + "Requested by: " + requestedBy.getEmail() + "\n\n"
                    + "Consider running the POI ingestion pipeline for this city.";
            for (User admin : admins) {
                try {
                    emailService.sendEmail(admin.getEmail(), subject, body);
                } catch (Exception e) {
                    log.warn("Failed to notify admin {} about missing city \"{}\": {}",
                            admin.getEmail(), normalizedCity, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to notify admins about missing city \"{}\": {}", normalizedCity, e.getMessage());
        }
    }

    // Strips a trailing ", Country"/", Region" so "Rome, Italy" matches the ingested "city" column.
    // Public static so TripService.suggestStops() can reuse the same normalization.
    public static String extractCityName(String destination) {
        return destination.split(",", 2)[0].trim();
    }

    private record ValidationResult(GeneratedItineraryDraft draft, List<String> errors) {
    }

    // Exercises parse+validate against a hand-crafted response, without an LLM call.
    public List<String> validateRawJson(String rawJson, GenerateTripRequest request, List<PointOfInterest> candidates) {
        return parseAndValidate(rawJson, request, candidates).errors();
    }

    // Timed wrapper around parseAndValidate, used for both the first and repair attempts.
    private ValidationResult timedParseAndValidate(String rawJson, GenerateTripRequest request, List<PointOfInterest> candidates) {
        long start = System.currentTimeMillis();
        ValidationResult result = parseAndValidate(rawJson, request, candidates);
        long elapsedMs = System.currentTimeMillis() - start;

        log.info("Parsed/validated itinerary JSON in {}ms ({} error(s))", elapsedMs, result.errors().size());
        return result;
    }

    // Deserializes and validates the LLM's response; a parse failure is treated as a validation error.
    private ValidationResult parseAndValidate(String rawJson, GenerateTripRequest request, List<PointOfInterest> candidates) {
        GeneratedItineraryDraft draft;
        try {
            draft = objectMapper.readValue(rawJson, GeneratedItineraryDraft.class);
        } catch (Exception e) {
            return new ValidationResult(null, List.of("Response was not valid JSON: " + e.getMessage()));
        }
        return new ValidationResult(draft, validateDraft(draft, request, candidates));
    }

    // Checks: every poiId is a real candidate, dayNumbers are exactly 1..dayCount, startTime < endTime.
    private List<String> validateDraft(GeneratedItineraryDraft draft, GenerateTripRequest request, List<PointOfInterest> candidates) {
        List<String> errors = new ArrayList<>();
        if (draft == null || draft.days() == null || draft.days().isEmpty()) {
            errors.add("Response must include a non-empty \"days\" array.");
            return errors;
        }

        Set<Long> candidateIds = candidates.stream().map(PointOfInterest::getId).collect(Collectors.toSet());
        long dayCount = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;

        for (GeneratedDayDraft day : draft.days()) {
            if (day.dayNumber() == null || day.dayNumber() < 1 || day.dayNumber() > dayCount) {
                errors.add("dayNumber " + day.dayNumber() + " is out of range; expected 1 to " + dayCount + ".");
            }
            if (day.stops() == null) {
                errors.add("Day " + day.dayNumber() + " is missing a \"stops\" array.");
                continue;
            }
            // Prevents a repair retry from "succeeding" with a trivially valid but useless empty array.
            if (day.stops().isEmpty()) {
                errors.add("Day " + day.dayNumber() + " has no stops — every day must include at least one stop.");
                continue;
            }
            for (GeneratedStopDraft stop : day.stops()) {
                Long poiId = parsePoiId(stop.poiId());
                if (poiId == null || !candidateIds.contains(poiId)) {
                    errors.add("poiId " + stop.poiId() + " on day " + day.dayNumber() + " is not one of the candidate POI ids.");
                }

                LocalTime startTime = parseTimeOrNull(stop.startTime());
                LocalTime endTime = parseTimeOrNull(stop.endTime());
                if (startTime == null || endTime == null) {
                    errors.add("Stop with poiId " + stop.poiId() + " on day " + day.dayNumber()
                            + " has an invalid time format (expected \"HH:mm\"): startTime=" + stop.startTime()
                            + ", endTime=" + stop.endTime() + ".");
                } else if (!startTime.isBefore(endTime)) {
                    errors.add("Stop with poiId " + stop.poiId() + " on day " + day.dayNumber()
                            + " has startTime (" + stop.startTime() + ") not before endTime (" + stop.endTime() + ").");
                }
            }
        }

        Set<Integer> distinctDayNumbers = draft.days().stream()
                .map(GeneratedDayDraft::dayNumber)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
        List<Integer> expectedDayNumbers = IntStream.rangeClosed(1, (int) dayCount).boxed().toList();
        if (!new ArrayList<>(distinctDayNumbers).equals(expectedDayNumbers)) {
            errors.add("Expected exactly one entry per day with dayNumber 1.." + dayCount
                    + ", got dayNumbers " + distinctDayNumbers + ".");
        }

        return errors;
    }

    private LocalTime parseTimeOrNull(String time) {
        if (time == null) {
            return null;
        }
        try {
            return LocalTime.parse(time);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private Long parsePoiId(String poiId) {
        if (poiId == null) {
            return null;
        }
        try {
            return Long.parseLong(poiId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Original prompt + broken response + specific errors, asking for a corrected response.
    private String buildRepairPrompt(String originalPrompt, String originalResponse, List<String> errors) {
        return originalPrompt + "\n\n"
                + "You previously responded with the following JSON, but it has validation errors:\n\n"
                + originalResponse + "\n\n"
                + "Validation errors:\n- " + String.join("\n- ", errors) + "\n\n"
                + "Fix ONLY these errors and return the corrected JSON in the exact same shape. "
                + "Respond with ONLY valid JSON, no markdown fences, no extra commentary.";
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String trimmed = text.strip();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength).stripTrailing() + "...";
    }

    private String buildRequestSummary(GenerateTripRequest request) {
        return "%s. Interests: %s. Avoid: %s. Pace: %s.".formatted(
                request.getDestination(),
                String.join(", ", request.getInterests()),
                String.join(", ", request.getAvoid()),
                request.getPace());
    }
}
