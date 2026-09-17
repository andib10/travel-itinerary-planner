package com.app.travel_planner.controller;

import com.app.travel_planner.dto.CreateStopRequest;
import com.app.travel_planner.dto.StopResponse;
import com.app.travel_planner.entity.User;
import com.app.travel_planner.service.TripService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/days")
public class DayController {

    private final TripService tripService;

    public DayController(TripService tripService) {
        this.tripService = tripService;
    }

    @PostMapping("/{dayId}/stops")
    @ResponseStatus(HttpStatus.CREATED)
    public StopResponse addStop(@PathVariable Long dayId, @Valid @RequestBody CreateStopRequest request,
                                 @AuthenticationPrincipal User currentUser) {
        return tripService.addStop(dayId, request, currentUser);
    }
}
