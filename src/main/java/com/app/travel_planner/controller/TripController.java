package com.app.travel_planner.controller;

import com.app.travel_planner.dto.CreateDayRequest;
import com.app.travel_planner.dto.CreateTripRequest;
import com.app.travel_planner.dto.GenerateTripRequest;
import com.app.travel_planner.dto.ItineraryDayResponse;
import com.app.travel_planner.dto.OptimizeDayResponse;
import com.app.travel_planner.dto.SuggestedStopResponse;
import com.app.travel_planner.dto.TripDetailResponse;
import com.app.travel_planner.dto.TripSummaryResponse;
import com.app.travel_planner.entity.User;
import com.app.travel_planner.service.ItineraryGenerationService;
import com.app.travel_planner.service.TripService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;
    private final ItineraryGenerationService itineraryGenerationService;

    public TripController(TripService tripService, ItineraryGenerationService itineraryGenerationService) {
        this.tripService = tripService;
        this.itineraryGenerationService = itineraryGenerationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TripSummaryResponse createTrip(@Valid @RequestBody CreateTripRequest request,
                                           @AuthenticationPrincipal User currentUser) {
        return tripService.createTrip(request, currentUser);
    }

    @GetMapping
    public List<TripSummaryResponse> listTrips(@AuthenticationPrincipal User currentUser) {
        return tripService.listTrips(currentUser);
    }

    @GetMapping("/{id}")
    public TripDetailResponse getTrip(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        return tripService.getTripDetail(id, currentUser);
    }

    @DeleteMapping("/{id}")
    public void deleteTrip(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        tripService.deleteTrip(id, currentUser);
    }

    @PostMapping("/{tripId}/days")
    @ResponseStatus(HttpStatus.CREATED)
    public ItineraryDayResponse addDay(@PathVariable Long tripId, @Valid @RequestBody CreateDayRequest request,
                                        @AuthenticationPrincipal User currentUser) {
        return tripService.addDay(tripId, request, currentUser);
    }

    // Re-run resequence + resolve on a day's current stops, without the LLM. Opt-in only.
    @PostMapping("/{tripId}/days/{dayId}/optimize")
    public OptimizeDayResponse optimizeDay(@PathVariable Long tripId, @PathVariable Long dayId,
                                            @RequestParam(required = false) Long resetTimeForStopId,
                                            @RequestParam(defaultValue = "false") boolean allowGapCompression,
                                            @AuthenticationPrincipal User currentUser) {
        return tripService.optimizeDay(tripId, dayId, currentUser, resetTimeForStopId, allowGapCompression);
    }

    // "Add a stop" picker: POIs for this trip's city not already used in the trip.
    @GetMapping("/{tripId}/days/{dayId}/suggestions")
    public List<SuggestedStopResponse> suggestStops(@PathVariable Long tripId, @PathVariable Long dayId,
                                                      @AuthenticationPrincipal User currentUser) {
        return tripService.suggestStops(tripId, dayId, currentUser);
    }

    // RAG pipeline: embed -> retrieve -> prompt -> LLM generate -> validate/repair -> save.
    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public TripDetailResponse generateTrip(@Valid @RequestBody GenerateTripRequest request,
                                            @AuthenticationPrincipal User currentUser) {
        return itineraryGenerationService.generateTrip(request, currentUser);
    }
}
