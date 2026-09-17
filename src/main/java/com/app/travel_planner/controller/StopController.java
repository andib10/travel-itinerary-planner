package com.app.travel_planner.controller;

import com.app.travel_planner.dto.ReorderStopRequest;
import com.app.travel_planner.dto.StopResponse;
import com.app.travel_planner.entity.User;
import com.app.travel_planner.service.TripService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stops")
public class StopController {

    private final TripService tripService;

    public StopController(TripService tripService) {
        this.tripService = tripService;
    }

    @DeleteMapping("/{id}")
    public void deleteStop(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        tripService.deleteStop(id, currentUser);
    }

    @PutMapping("/{id}/reorder")
    public StopResponse reorderStop(@PathVariable Long id, @Valid @RequestBody ReorderStopRequest request,
                                     @AuthenticationPrincipal User currentUser) {
        return tripService.reorderStop(id, request, currentUser);
    }
}
