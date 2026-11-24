package com.kharcha.auth.controller;

import com.kharcha.auth.dto.AuthResponse;
import com.kharcha.auth.dto.LoginRequest;
import com.kharcha.auth.dto.DeleteAccountRequest;
import com.kharcha.auth.dto.RegisterRequest;
import com.kharcha.auth.dto.UserProfileResponse;
import com.kharcha.auth.service.AuthService;
import com.kharcha.auth.service.JwtService;
import com.kharcha.auth.service.LoginRateLimiter;
import com.kharcha.common.error.InvalidCredentialsException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User registration, login, and token management")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final boolean cookieSecure;
    private final LoginRateLimiter loginRateLimiter;

    public AuthController(
            AuthService authService,
            JwtService jwtService,
            LoginRateLimiter loginRateLimiter,
            @Value("${jwt.cookie-secure:true}") boolean cookieSecure
    ) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.loginRateLimiter = loginRateLimiter;
        this.cookieSecure = cookieSecure;
    }

    @PostMapping("/register")
    @Operation(summary = "Register user", description = "Creates a new user account with email and password")
    public ResponseEntity<UserProfileResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserProfileResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticates user and returns JWT access token plus refresh cookie")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        if (!loginRateLimiter.allowAttempt(httpRequest.getRemoteAddr(), request.email())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "RATE_LIMITED", "message", "Too many sign-in attempts. Try again in 15 minutes."));
        }
        AuthService.AuthResult result;
        try {
            result = authService.login(request);
            loginRateLimiter.recordSuccess(request.email());
        } catch (InvalidCredentialsException invalidCredentials) {
            loginRateLimiter.recordFailure(httpRequest.getRemoteAddr(), request.email());
            throw invalidCredentials;
        }
        ResponseCookie cookie = buildRefreshTokenCookie(result.refreshToken(), jwtService.getRefreshTokenExpirationSeconds());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(result.authResponse());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Uses refresh token cookie to generate new access token")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken
    ) {
        AuthService.AuthResult result = authService.refreshToken(refreshToken);
        ResponseCookie cookie = buildRefreshTokenCookie(result.refreshToken(), jwtService.getRefreshTokenExpirationSeconds());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(result.authResponse());
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Returns profile of authenticated user")
    public ResponseEntity<UserProfileResponse> getCurrentUser(@AuthenticationPrincipal UUID userId) {
        UserProfileResponse response = authService.getUserProfile(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Revokes the refresh session and clears its cookie")
    public ResponseEntity<Map<String, String>> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshToken
    ) {
        authService.logout(refreshToken);
        ResponseCookie cookie = buildRefreshTokenCookie("", 0);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of("message", "Successfully logged out"));
    }

    @DeleteMapping("/account")
    @Operation(summary = "Delete current account", description = "Deletes the authenticated user's account and data after email confirmation")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal UUID userId,
                                              @Valid @RequestBody DeleteAccountRequest request) {
        authService.deleteAccount(userId, request.email());
        ResponseCookie cookie = buildRefreshTokenCookie("", 0);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }

    private ResponseCookie buildRefreshTokenCookie(String token, long maxAgeSeconds) {
        return ResponseCookie.from("refreshToken", token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(maxAgeSeconds)
                .build();
    }
}
