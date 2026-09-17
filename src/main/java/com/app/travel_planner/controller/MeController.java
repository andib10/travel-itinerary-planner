package com.app.travel_planner.controller;

import com.app.travel_planner.dto.ChangePasswordRequest;
import com.app.travel_planner.entity.User;
import com.app.travel_planner.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Returns the authenticated user's identity (id, email, role) for the frontend.
 */
@RestController
public class MeController {

    private final AuthService authService;

    public MeController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/api/me")
    public Map<String, Object> me(@AuthenticationPrincipal User user) {
        return Map.of("id", user.getId(), "email", user.getEmail(), "role", user.getRole(),
                "emailVerified", user.isEmailVerified());
    }

    // No response body; frontend logs the user out to force re-authentication.
    @PutMapping("/api/me/password")
    public void changePassword(@AuthenticationPrincipal User user, @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(user, request);
    }
}
