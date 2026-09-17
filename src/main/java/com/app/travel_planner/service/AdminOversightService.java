package com.app.travel_planner.service;

import com.app.travel_planner.dto.AdminTripResponse;
import com.app.travel_planner.dto.AdminUserResponse;
import com.app.travel_planner.entity.Trip;
import com.app.travel_planner.entity.User;
import com.app.travel_planner.exception.InvalidRoleException;
import com.app.travel_planner.exception.ResourceNotFoundException;
import com.app.travel_planner.exception.SelfActionNotAllowedException;
import com.app.travel_planner.repository.TripRepository;
import com.app.travel_planner.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Read-only user/trip listing across all users for the admin surface. Full trip detail and
 * delete live on {@link TripService} instead.
 */
@Service
@Transactional
public class AdminOversightService {

    private static final Set<String> VALID_ROLES = Set.of("USER", "ADMIN");

    private final UserRepository userRepository;
    private final TripRepository tripRepository;

    public AdminOversightService(UserRepository userRepository, TripRepository tripRepository) {
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
    }

    public List<AdminUserResponse> listUsers() {
        return userRepository.findAll().stream().map(this::toUserResponse).toList();
    }

    public List<AdminTripResponse> listTrips() {
        return tripRepository.findAll().stream().map(this::toTripResponse).toList();
    }

    // promote/demote a user's role; blocks acting on your own account to avoid self-lockout.
    public AdminUserResponse updateUserRole(Long userId, String newRole, User currentUser) {
        if (userId.equals(currentUser.getId())) {
            throw new SelfActionNotAllowedException("You cannot change your own role");
        }
        if (!VALID_ROLES.contains(newRole)) {
            throw new InvalidRoleException(newRole);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        user.setRole(newRole);
        userRepository.save(user);
        return toUserResponse(user);
    }

    // Same self-action guard as above. Deletion cascades to the user's trips and their days/stops.
    public void deleteUser(Long userId, User currentUser) {
        if (userId.equals(currentUser.getId())) {
            throw new SelfActionNotAllowedException("You cannot delete your own account");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        userRepository.delete(user);
    }

    private AdminUserResponse toUserResponse(User user) {
        return new AdminUserResponse(user.getId(), user.getEmail(), user.getRole(), user.getCreatedAt(),
                user.getTrips().size());
    }

    private AdminTripResponse toTripResponse(Trip trip) {
        return new AdminTripResponse(trip.getId(), trip.getUser().getEmail(), trip.getDestination(),
                trip.getStartDate(), trip.getEndDate(), trip.getStatus(), trip.getItineraryDays().size());
    }
}
