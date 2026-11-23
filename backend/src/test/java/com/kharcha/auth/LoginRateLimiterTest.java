package com.kharcha.auth;

import com.kharcha.auth.service.LoginRateLimiter;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRateLimiterTest {
    @Test
    void limitsByIpAndEmailWithinFifteenMinutes() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-15T10:00:00Z"), ZoneId.of("UTC"));
        LoginRateLimiter limiter = new LoginRateLimiter(clock);
        for (int attempt = 0; attempt < 5; attempt++) {
            assertThat(limiter.allowAttempt("127.0.0.1", "a@example.com")).isTrue();
            limiter.recordFailure("127.0.0.1", "a@example.com");
        }
        assertThat(limiter.allowAttempt("127.0.0.1", "a@example.com")).isFalse();
        assertThat(limiter.allowAttempt("127.0.0.2", "a@example.com")).isFalse();
    }
}
