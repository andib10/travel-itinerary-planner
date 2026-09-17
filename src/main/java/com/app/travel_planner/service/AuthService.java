package com.app.travel_planner.service;

import com.app.travel_planner.dto.AuthResponse;
import com.app.travel_planner.dto.ChangePasswordRequest;
import com.app.travel_planner.dto.ForgotPasswordRequest;
import com.app.travel_planner.dto.LoginRequest;
import com.app.travel_planner.dto.RegisterRequest;
import com.app.travel_planner.dto.ResetPasswordRequest;
import com.app.travel_planner.entity.User;
import com.app.travel_planner.exception.EmailAlreadyExistsException;
import com.app.travel_planner.exception.InvalidOrExpiredTokenException;
import com.app.travel_planner.repository.UserRepository;
import com.app.travel_planner.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    // Longer than reset token: a missed verification email is lower-stakes to re-request.
    private static final long VERIFICATION_TOKEN_TTL_HOURS = 24;

    // Shorter than verification token: a reset link grants a password change, so tighter window.
    private static final long RESET_TOKEN_TTL_MINUTES = 60;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final String frontendBaseUrl;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                        EmailService emailService, @Value("${app.frontend.base-url}") String frontendBaseUrl) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    /**
     * Registers the user and immediately logs them in, even though {@code emailVerified} starts false.
     */
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        user.setRole("USER");
        user.setEmailVerified(false);
        user.setVerificationToken(UUID.randomUUID().toString());
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(VERIFICATION_TOKEN_TTL_HOURS));

        userRepository.save(user);
        sendVerificationEmail(user);

        return new AuthResponse(jwtService.generateToken(user));
    }

    private void sendVerificationEmail(User user) {
        String link = frontendBaseUrl + "/verify-email?token=" + user.getVerificationToken();
        emailService.sendEmail(user.getEmail(), "Verify your email — Travel Planner",
                "Welcome to Travel Planner! Please verify your email by clicking the link below:\n\n"
                        + link + "\n\nThis link expires in " + VERIFICATION_TOKEN_TTL_HOURS + " hours.");
    }

    /**
     * Looks up the user by token alone; clears both token fields on success so it can't be replayed.
     */
    public void verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new InvalidOrExpiredTokenException("Invalid or expired verification link."));
        if (user.getVerificationTokenExpiry() == null || user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new InvalidOrExpiredTokenException("Invalid or expired verification link.");
        }
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        userRepository.save(user);
    }

    /**
     * Deliberately silent on whether the email exists, to avoid leaking account existence.
     */
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            user.setResetToken(UUID.randomUUID().toString());
            user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(RESET_TOKEN_TTL_MINUTES));
            userRepository.save(user);

            String link = frontendBaseUrl + "/reset-password?token=" + user.getResetToken();
            emailService.sendEmail(user.getEmail(), "Reset your password — Travel Planner",
                    "We received a request to reset your Travel Planner password. Click the link below to choose a new one:\n\n"
                            + link + "\n\nThis link expires in " + RESET_TOKEN_TTL_MINUTES
                            + " minutes. If you didn't request this, you can safely ignore this email.");
        });
    }

    /**
     * Single-use: clears the token on success, so a reused token is rejected like an unknown one.
     */
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByResetToken(request.getToken())
                .orElseThrow(() -> new InvalidOrExpiredTokenException("Invalid or expired reset link."));
        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new InvalidOrExpiredTokenException("Invalid or expired reset link.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        return new AuthResponse(jwtService.generateToken(user));
    }

    // currentUser is already loaded via @AuthenticationPrincipal, no separate lookup needed.
    public void changePassword(User currentUser, ChangePasswordRequest request) {
        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }
        currentUser.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(currentUser);
    }
}
