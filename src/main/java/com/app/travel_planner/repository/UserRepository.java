package com.app.travel_planner.repository;

import com.app.travel_planner.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByVerificationToken(String verificationToken);

    Optional<User> findByResetToken(String resetToken);

    // Used to notify every admin when a user requests a destination with no ingested POI data.
    List<User> findByRole(String role);
}
