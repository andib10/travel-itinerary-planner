package com.app.travel_planner.controller;

import com.app.travel_planner.dto.AdminTripResponse;
import com.app.travel_planner.dto.AdminUserResponse;
import com.app.travel_planner.dto.TripDetailResponse;
import com.app.travel_planner.dto.UpdateUserRoleRequest;
import com.app.travel_planner.entity.User;
import com.app.travel_planner.service.AdminOversightService;
import com.app.travel_planner.service.TripService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin user/trip oversight
 */
@RestController
@RequestMapping("/api/admin")
public class AdminOversightController {

    private final AdminOversightService adminOversightService;
    private final TripService tripService;

    public AdminOversightController(AdminOversightService adminOversightService, TripService tripService) {
        this.adminOversightService = adminOversightService;
        this.tripService = tripService;
    }

    @GetMapping("/users")
    public List<AdminUserResponse> listUsers() {
        return adminOversightService.listUsers();
    }

    // Promote/demote a user's role; blocked on the caller's own account.
    @PutMapping("/users/{id}/role")
    public AdminUserResponse updateUserRole(@PathVariable Long id, @Valid @RequestBody UpdateUserRoleRequest request,
                                             @AuthenticationPrincipal User currentUser) {
        return adminOversightService.updateUserRole(id, request.getRole(), currentUser);
    }

    @DeleteMapping("/users/{id}")
    public void deleteUser(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        adminOversightService.deleteUser(id, currentUser);
    }

    @GetMapping("/trips")
    public List<AdminTripResponse> listTrips() {
        return adminOversightService.listTrips();
    }

    @GetMapping("/trips/{id}")
    public TripDetailResponse getTrip(@PathVariable Long id) {
        return tripService.getTripDetailForAdmin(id);
    }

    @DeleteMapping("/trips/{id}")
    public void deleteTrip(@PathVariable Long id) {
        tripService.deleteTripForAdmin(id);
    }
}
