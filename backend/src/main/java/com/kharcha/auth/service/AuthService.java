package com.kharcha.auth.service;

import com.kharcha.auth.domain.User;
import com.kharcha.auth.dto.AuthResponse;
import com.kharcha.auth.dto.LoginRequest;
import com.kharcha.auth.dto.RegisterRequest;
import com.kharcha.auth.dto.UserProfileResponse;
import com.kharcha.auth.repository.UserRepository;
import com.kharcha.common.error.DuplicateEmailException;
import com.kharcha.common.error.InvalidCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public record AuthResult(AuthResponse authResponse, String refreshToken) {}

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserProfileResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().toLowerCase().trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException("An account with this email already exists");
        }

        User user = new User(
                UUID.randomUUID(),
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                Instant.now(),
                200000L
        );

        User savedUser = userRepository.save(user);
        return new UserProfileResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getCreatedAt(),
                savedUser.getAgentCapPaise()
        );
    }

    @Transactional(readOnly = true)
    public AuthResult login(LoginRequest request) {
        String normalizedEmail = request.email().toLowerCase().trim();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());

        AuthResponse authResponse = AuthResponse.bearer(accessToken, jwtService.getAccessTokenExpirationSeconds());
        return new AuthResult(authResponse, refreshToken);
    }

    @Transactional(readOnly = true)
    public AuthResult refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank() || !jwtService.isTokenValid(refreshToken)) {
            throw new InvalidCredentialsException("Invalid or expired refresh token");
        }

        String tokenType = jwtService.extractTokenType(refreshToken);
        if (!"REFRESH".equalsIgnoreCase(tokenType)) {
            throw new InvalidCredentialsException("Invalid token type");
        }

        UUID userId = jwtService.extractUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String newRefreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());

        AuthResponse authResponse = AuthResponse.bearer(newAccessToken, jwtService.getAccessTokenExpirationSeconds());
        return new AuthResult(authResponse, newRefreshToken);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getCreatedAt(),
                user.getAgentCapPaise()
        );
    }
}
