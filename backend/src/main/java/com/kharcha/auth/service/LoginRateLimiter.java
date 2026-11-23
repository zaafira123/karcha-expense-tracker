package com.kharcha.auth.service;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/** In-process sliding-window limiter for login attempts by both source IP and email. */
@Component
public class LoginRateLimiter {
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private final Clock clock;
    private final Map<String, Deque<Instant>> attempts = new HashMap<>();

    public LoginRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public synchronized boolean allowAttempt(String ipAddress, String email) {
        Instant now = Instant.now(clock);
        String ipKey = "ip:" + ipAddress;
        String emailKey = "email:" + email.trim().toLowerCase();
        cleanup(now);
        if (isLimited(ipKey, now) || isLimited(emailKey, now)) return false;
        return true;
    }

    public synchronized void recordFailure(String ipAddress, String email) {
        Instant now = Instant.now(clock);
        cleanup(now);
        attempts.computeIfAbsent("ip:" + ipAddress, ignored -> new ArrayDeque<>()).addLast(now);
        attempts.computeIfAbsent("email:" + email.trim().toLowerCase(), ignored -> new ArrayDeque<>()).addLast(now);
    }

    public synchronized void recordSuccess(String email) {
        attempts.remove("email:" + email.trim().toLowerCase());
    }

    private boolean isLimited(String key, Instant now) {
        Deque<Instant> entries = attempts.get(key);
        trim(entries, now);
        return entries != null && entries.size() >= MAX_ATTEMPTS;
    }

    private void cleanup(Instant now) {
        attempts.values().forEach(entries -> trim(entries, now));
        attempts.values().removeIf(Deque::isEmpty);
    }

    private void trim(Deque<Instant> entries, Instant now) {
        if (entries == null) return;
        Instant cutoff = now.minus(WINDOW);
        while (!entries.isEmpty() && !entries.peekFirst().isAfter(cutoff)) entries.removeFirst();
    }
}
