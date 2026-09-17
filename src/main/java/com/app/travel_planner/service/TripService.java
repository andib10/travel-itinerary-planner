package com.app.travel_planner.service;

import com.app.travel_planner.dto.CreateDayRequest;
import com.app.travel_planner.dto.CreateStopRequest;
import com.app.travel_planner.dto.CreateTripRequest;
import com.app.travel_planner.dto.ItineraryDayResponse;
import com.app.travel_planner.dto.OptimizeDayResponse;
import com.app.travel_planner.dto.ReorderStopRequest;
import com.app.travel_planner.dto.StopResponse;
import com.app.travel_planner.dto.SuggestedStopResponse;
import com.app.travel_planner.dto.TripDetailResponse;
import com.app.travel_planner.dto.TripSummaryResponse;
import com.app.travel_planner.entity.ItineraryDay;
import com.app.travel_planner.entity.PointOfInterest;
import com.app.travel_planner.entity.Stop;
import com.app.travel_planner.entity.Trip;
import com.app.travel_planner.entity.User;
import com.app.travel_planner.exception.ResourceNotFoundException;
import com.app.travel_planner.optimization.DayOptimizer;
import com.app.travel_planner.optimization.OptimizationResult;
import com.app.travel_planner.repository.ItineraryDayRepository;
import com.app.travel_planner.repository.PointOfInterestRepository;
import com.app.travel_planner.repository.StopRepository;
import com.app.travel_planner.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manual trip/day/stop CRUD. Every method enforces ownership, reporting another user's
 * trip/day/stop as not found (404) rather than forbidden, to avoid leaking existence.
 */
@Service
@Transactional
public class TripService {

    private final TripRepository tripRepository;
    private final ItineraryDayRepository itineraryDayRepository;
    private final StopRepository stopRepository;
    private final PointOfInterestRepository pointOfInterestRepository;

    public TripService(TripRepository tripRepository, ItineraryDayRepository itineraryDayRepository,
                        StopRepository stopRepository, PointOfInterestRepository pointOfInterestRepository) {
        this.tripRepository = tripRepository;
        this.itineraryDayRepository = itineraryDayRepository;
        this.stopRepository = stopRepository;
        this.pointOfInterestRepository = pointOfInterestRepository;
    }

    public TripSummaryResponse createTrip(CreateTripRequest request, User currentUser) {
        Trip trip = new Trip();
        trip.setUser(currentUser);
        trip.setDestination(request.getDestination());
        trip.setStartDate(request.getStartDate());
        trip.setEndDate(request.getEndDate());
        trip.setBudget(request.getBudget());
        trip.setStatus("DRAFT");
        trip.setCreatedAt(LocalDateTime.now());
        tripRepository.save(trip);
        return toSummary(trip);
    }

    public List<TripSummaryResponse> listTrips(User currentUser) {
        return tripRepository.findByUserId(currentUser.getId()).stream()
                .map(this::toSummary)
                .toList();
    }

    public TripDetailResponse getTripDetail(Long tripId, User currentUser) {
        Trip trip = getOwnedTrip(tripId, currentUser);
        return toDetail(trip);
    }

    public void deleteTrip(Long tripId, User currentUser) {
        Trip trip = getOwnedTrip(tripId, currentUser);
        tripRepository.delete(trip); // cascades to days/stops
    }

    // Used by ItineraryGenerationService once a generated trip's days/stops are saved.
    public void markGenerated(Long tripId, User currentUser) {
        Trip trip = getOwnedTrip(tripId, currentUser);
        trip.setStatus("GENERATED");
        tripRepository.save(trip);
    }

    public ItineraryDayResponse addDay(Long tripId, CreateDayRequest request, User currentUser) {
        Trip trip = getOwnedTrip(tripId, currentUser);
        ItineraryDay day = new ItineraryDay();
        day.setTrip(trip);
        day.setDayNumber(request.getDayNumber());
        day.setDate(request.getDate());
        itineraryDayRepository.save(day);
        // Keep the inverse side in sync for callers sharing this transaction/persistence context.
        trip.getItineraryDays().add(day);
        return toDayResponse(day);
    }

    public StopResponse addStop(Long dayId, CreateStopRequest request, User currentUser) {
        ItineraryDay day = getOwnedDay(dayId, currentUser);
        PointOfInterest poi = pointOfInterestRepository.findById(request.getPoiId())
                .orElseThrow(() -> new ResourceNotFoundException("Point of interest not found: " + request.getPoiId()));

        Stop stop = new Stop();
        stop.setItineraryDay(day);
        stop.setPointOfInterest(poi);
        stop.setStartTime(request.getStartTime());
        stop.setEndTime(request.getEndTime());
        stop.setNotes(request.getNotes());
        stop.setOrderIndex(request.getOrderIndex());
        stopRepository.save(stop);
        day.getStops().add(stop); // keep the inverse side in sync
        return toStopResponse(stop);
    }

    /**
     * "Add a stop" picker: real, already-ingested POIs for this trip's city not already used in it.
     */
    public List<SuggestedStopResponse> suggestStops(Long tripId, Long dayId, User currentUser) {
        ItineraryDay day = getOwnedDay(dayId, currentUser);
        Trip trip = day.getTrip();
        if (!trip.getId().equals(tripId)) {
            // Catches a dayId that's valid but doesn't belong to this tripId.
            throw new ResourceNotFoundException("Day not found: " + dayId);
        }

        String city = ItineraryGenerationService.extractCityName(trip.getDestination());
        // Excludes a POI already scheduled on ANY day of this trip, not just this one.
        Set<Long> alreadyUsedPoiIds = trip.getItineraryDays().stream()
                .flatMap(d -> d.getStops().stream())
                .map(stop -> stop.getPointOfInterest().getId())
                .collect(Collectors.toSet());

        return pointOfInterestRepository.findByCityIgnoreCase(city).stream()
                .filter(poi -> !alreadyUsedPoiIds.contains(poi.getId()))
                .sorted(Comparator.comparing(PointOfInterest::getCategory).thenComparing(PointOfInterest::getName))
                .map(this::toSuggestedStopResponse)
                .toList();
    }

    public void deleteStop(Long stopId, User currentUser) {
        Stop stop = getOwnedStop(stopId, currentUser);
        stopRepository.delete(stop);
    }

    public StopResponse reorderStop(Long stopId, ReorderStopRequest request, User currentUser) {
        Stop stop = getOwnedStop(stopId, currentUser);

        List<Stop> siblings = new ArrayList<>(stop.getItineraryDay().getStops());
        siblings.sort(Comparator.comparingInt(Stop::getOrderIndex));
        siblings.removeIf(s -> s.getId().equals(stop.getId()));

        int maxPosition = siblings.size() + 1; // positions available once moved stop is reinserted
        int targetPosition = Math.max(1, Math.min(request.getNewOrderIndex(), maxPosition));
        siblings.add(targetPosition - 1, stop);

        for (int i = 0; i < siblings.size(); i++) {
            siblings.get(i).setOrderIndex(i + 1);
        }
        stopRepository.saveAll(siblings);
        return toStopResponse(stop);
    }

    // Standalone re-optimize, opt-in only so it never overrides a user's manual reorder.
    // Uses optimizeDayTimingOnly(): order is fixed, only times shift.
    public OptimizeDayResponse optimizeDay(Long tripId, Long dayId, User currentUser) {
        return optimizeDay(tripId, dayId, currentUser, null);
    }

    // resetTimeForStopId: a just-moved stop whose time should be recomputed fresh, not anchored
    // to its old position. null preserves prior behavior.
    public OptimizeDayResponse optimizeDay(Long tripId, Long dayId, User currentUser, Long resetTimeForStopId) {
        return optimizeDay(tripId, dayId, currentUser, resetTimeForStopId, false);
    }

    // allowGapCompression: opt-in, e.g. when the frontend asks to close a large idle gap.
    // false preserves prior behavior.
    public OptimizeDayResponse optimizeDay(Long tripId, Long dayId, User currentUser, Long resetTimeForStopId,
                                            boolean allowGapCompression) {
        ItineraryDay day = getOwnedDay(dayId, currentUser);
        if (!day.getTrip().getId().equals(tripId)) {
            // Guards against a dayId that's valid but doesn't belong to the tripId in the URL.
            throw new ResourceNotFoundException("Day not found: " + dayId);
        }

        List<Stop> currentOrderStops = new ArrayList<>(day.getStops());
        currentOrderStops.sort(Comparator.comparingInt(Stop::getOrderIndex));

        // Mutates startTime/endTime in place; orderIndex untouched.
        OptimizationResult result =
                DayOptimizer.optimizeDayTimingOnly(currentOrderStops, resetTimeForStopId, allowGapCompression);
        stopRepository.saveAll(currentOrderStops);

        return new OptimizeDayResponse(toDayResponse(day), result);
    }

    // Admin oversight: full detail for any user's trip, skipping the ownership check
    // (access is restricted to ADMIN at the route level instead).
    public TripDetailResponse getTripDetailForAdmin(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found: " + tripId));
        return toDetail(trip);
    }

    // Admin oversight: delete any user's trip, same cascade as deleteTrip() but no ownership check.
    public void deleteTripForAdmin(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found: " + tripId));
        tripRepository.delete(trip);
    }

    // --- ownership lookups: exist-and-belongs-to-currentUser, else 404 either way ---

    private Trip getOwnedTrip(Long tripId, User currentUser) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found: " + tripId));
        if (!trip.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Trip not found: " + tripId);
        }
        return trip;
    }

    private ItineraryDay getOwnedDay(Long dayId, User currentUser) {
        ItineraryDay day = itineraryDayRepository.findById(dayId)
                .orElseThrow(() -> new ResourceNotFoundException("Day not found: " + dayId));
        if (!day.getTrip().getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Day not found: " + dayId);
        }
        return day;
    }

    private Stop getOwnedStop(Long stopId, User currentUser) {
        Stop stop = stopRepository.findById(stopId)
                .orElseThrow(() -> new ResourceNotFoundException("Stop not found: " + stopId));
        if (!stop.getItineraryDay().getTrip().getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Stop not found: " + stopId);
        }
        return stop;
    }

    // --- entity -> DTO mapping ---

    private TripSummaryResponse toSummary(Trip trip) {
        return new TripSummaryResponse(trip.getId(), trip.getDestination(), trip.getStartDate(),
                trip.getEndDate(), trip.getBudget(), trip.getStatus());
    }

    private TripDetailResponse toDetail(Trip trip) {
        List<ItineraryDayResponse> days = trip.getItineraryDays().stream()
                .sorted(Comparator.comparingInt(ItineraryDay::getDayNumber))
                .map(this::toDayResponse)
                .toList();
        return new TripDetailResponse(trip.getId(), trip.getDestination(), trip.getStartDate(),
                trip.getEndDate(), trip.getBudget(), trip.getStatus(), days);
    }

    private ItineraryDayResponse toDayResponse(ItineraryDay day) {
        List<StopResponse> stops = day.getStops().stream()
                .sorted(Comparator.comparingInt(Stop::getOrderIndex))
                .map(this::toStopResponse)
                .toList();
        return new ItineraryDayResponse(day.getId(), day.getDayNumber(), day.getDate(), stops);
    }

    private StopResponse toStopResponse(Stop stop) {
        PointOfInterest poi = stop.getPointOfInterest();
        return new StopResponse(stop.getId(), poi.getId(), poi.getName(), poi.getLat(), poi.getLng(),
                stop.getStartTime(), stop.getEndTime(), stop.getNotes(), stop.getOrderIndex());
    }

    private SuggestedStopResponse toSuggestedStopResponse(PointOfInterest poi) {
        return new SuggestedStopResponse(poi.getId(), poi.getName(), poi.getCategory(), poi.getDescription(),
                poi.getLat(), poi.getLng());
    }
}
