package com.kharcha.parsing;

import com.kharcha.parsing.engine.DateParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class DateParserTest {

    private Clock fixedClock;
    // Tuesday, 10 Nov 2026
    private final LocalDate today = LocalDate.of(2026, 11, 10);

    @BeforeEach
    void setUp() {
        Instant fixedInstant = today.atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant();
        fixedClock = Clock.fixed(fixedInstant, ZoneId.of("Asia/Kolkata"));
    }

    @Test
    @DisplayName("Defaults to today when no date keyword is present")
    void testDefaultToday() {
        DateParser.DateParseResult result = DateParser.parseDate("paid 450 for pizza", fixedClock);
        assertThat(result.date()).isEqualTo(today);
        assertThat(result.isFutureFallback()).isFalse();
        assertThat(result.isExplicitDate()).isFalse();
    }

    @Test
    @DisplayName("Parses 'today' explicitly")
    void testToday() {
        DateParser.DateParseResult result = DateParser.parseDate("paid 12000 for rent today", fixedClock);
        assertThat(result.date()).isEqualTo(today);
        assertThat(result.isFutureFallback()).isFalse();
        assertThat(result.isExplicitDate()).isTrue();
    }

    @Test
    @DisplayName("Parses 'yesterday'")
    void testYesterday() {
        DateParser.DateParseResult result = DateParser.parseDate("spent 1,200 at dmart yesterday", fixedClock);
        assertThat(result.date()).isEqualTo(LocalDate.of(2026, 11, 9));
        assertThat(result.isFutureFallback()).isFalse();
        assertThat(result.isExplicitDate()).isTrue();
    }

    @Test
    @DisplayName("Parses weekday name to most recent past occurrence (not today)")
    void testWeekday() {
        // Today is Tuesday (10 Nov). Most recent Monday is yesterday (9 Nov).
        DateParser.DateParseResult mondayResult = DateParser.parseDate("rs 450 for dominos on monday", fixedClock);
        assertThat(mondayResult.date()).isEqualTo(LocalDate.of(2026, 11, 9));
        assertThat(mondayResult.isFutureFallback()).isFalse();

        // Most recent Tuesday is 7 days ago (3 Nov), not today
        DateParser.DateParseResult tuesdayResult = DateParser.parseDate("lunch on tuesday", fixedClock);
        assertThat(tuesdayResult.date()).isEqualTo(LocalDate.of(2026, 11, 3));
    }

    @Test
    @DisplayName("Parses calendar dates like '5 nov' or '5/11'")
    void testCalendarDates() {
        DateParser.DateParseResult result1 = DateParser.parseDate("bill paid on 5 nov", fixedClock);
        assertThat(result1.date()).isEqualTo(LocalDate.of(2026, 11, 5));
        assertThat(result1.isFutureFallback()).isFalse();

        DateParser.DateParseResult result2 = DateParser.parseDate("maintenance paid on 5/11", fixedClock);
        assertThat(result2.date()).isEqualTo(LocalDate.of(2026, 11, 5));
    }

    @Test
    @DisplayName("Parses ISO dates as year-month-day")
    void testIsoYearMonthDay() {
        DateParser.DateParseResult result = DateParser.parseDate(
                "debit on 2026-11-05", fixedClock);

        assertThat(result.date()).isEqualTo(LocalDate.of(2026, 11, 5));
        assertThat(result.isFutureFallback()).isFalse();
        assertThat(result.isExplicitDate()).isTrue();
    }

    @Test
    @DisplayName("Future dates fall back to today and flag future fallback")
    void testFutureDateFallback() {
        // 25 Dec is in the future relative to 10 Nov
        DateParser.DateParseResult result = DateParser.parseDate("paid 500 on 25 dec 2026 for gifts", fixedClock);
        assertThat(result.date()).isEqualTo(today);
        assertThat(result.isFutureFallback()).isTrue();

        DateParser.DateParseResult tomorrowResult = DateParser.parseDate("movie tomorrow", fixedClock);
        assertThat(tomorrowResult.date()).isEqualTo(today);
        assertThat(tomorrowResult.isFutureFallback()).isTrue();
    }
}
