package com.kharcha.parsing.engine;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DateParser {

    private DateParser() {}

    public record DateParseResult(LocalDate date, boolean isFutureFallback, boolean isExplicitDate) {}

    private static final Pattern YESTERDAY_PATTERN = Pattern.compile("(?i)\\byesterday\\b");
    private static final Pattern TODAY_PATTERN = Pattern.compile("(?i)\\btoday\\b");
    private static final Pattern TOMORROW_PATTERN = Pattern.compile("(?i)\\btomorrow\\b");

    private static final Pattern WEEKDAY_PATTERN = Pattern.compile(
            "(?i)\\b(?:on\\s+)?(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b"
    );

    // e.g. "5 nov", "on 5 nov", "5th november", "5 nov 2026"
    private static final Pattern DAY_MONTH_PATTERN = Pattern.compile(
            "(?i)\\b(?:on\\s+)?(\\d{1,2})(?:st|nd|rd|th)?\\s+(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)(?:\\s+(\\d{4}))?\\b"
    );

    // e.g. "nov 5", "on nov 5", "november 5th", "nov 5 2026"
    private static final Pattern MONTH_DAY_PATTERN = Pattern.compile(
            "(?i)\\b(?:on\\s+)?(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\\s+(\\d{1,2})(?:st|nd|rd|th)?(?:\\s+(\\d{4}))?\\b"
    );

    // e.g. "5/11", "05/11", "5-11", "5/11/2026" (DD/MM[/YYYY])
    private static final Pattern NUMERIC_DATE_PATTERN = Pattern.compile(
            "(?i)\\b(?:on\\s+)?(\\d{1,2})[/-](\\d{1,2})(?:[/-](\\d{2,4}))?\\b"
    );

    private static final Map<String, Integer> MONTH_NAMES = Map.ofEntries(
            Map.entry("jan", 1), Map.entry("january", 1),
            Map.entry("feb", 2), Map.entry("february", 2),
            Map.entry("mar", 3), Map.entry("march", 3),
            Map.entry("apr", 4), Map.entry("april", 4),
            Map.entry("may", 5),
            Map.entry("jun", 6), Map.entry("june", 6),
            Map.entry("jul", 7), Map.entry("july", 7),
            Map.entry("aug", 8), Map.entry("august", 8),
            Map.entry("sep", 9), Map.entry("sept", 9), Map.entry("september", 9),
            Map.entry("oct", 10), Map.entry("october", 10),
            Map.entry("nov", 11), Map.entry("november", 11),
            Map.entry("dec", 12), Map.entry("december", 12)
    );

    public static DateParseResult parseDate(String rawText, Clock clock) {
        LocalDate today = LocalDate.now(clock);
        if (rawText == null || rawText.isBlank()) {
            return new DateParseResult(today, false, false);
        }

        String text = rawText.trim();

        // 1. Check tomorrow explicitly (future date -> fallback to today, needsReview)
        if (TOMORROW_PATTERN.matcher(text).find()) {
            return new DateParseResult(today, true, true);
        }

        // 2. Check yesterday
        if (YESTERDAY_PATTERN.matcher(text).find()) {
            return new DateParseResult(today.minusDays(1), false, true);
        }

        // 3. Check today explicitly
        if (TODAY_PATTERN.matcher(text).find()) {
            return new DateParseResult(today, false, true);
        }

        // 4. Check Day + Month name (e.g., "5 nov", "on 5th november")
        Matcher dayMonthMatcher = DAY_MONTH_PATTERN.matcher(text);
        if (dayMonthMatcher.find()) {
            int day = Integer.parseInt(dayMonthMatcher.group(1));
            String monthStr = dayMonthMatcher.group(2).toLowerCase(Locale.ROOT);
            int month = MONTH_NAMES.getOrDefault(monthStr, 1);
            String yearStr = dayMonthMatcher.group(3);
            return resolveDateWithYearCheck(day, month, yearStr, today);
        }

        // 5. Check Month name + Day (e.g., "nov 5", "november 5th")
        Matcher monthDayMatcher = MONTH_DAY_PATTERN.matcher(text);
        if (monthDayMatcher.find()) {
            String monthStr = monthDayMatcher.group(1).toLowerCase(Locale.ROOT);
            int month = MONTH_NAMES.getOrDefault(monthStr, 1);
            int day = Integer.parseInt(monthDayMatcher.group(2));
            String yearStr = monthDayMatcher.group(3);
            return resolveDateWithYearCheck(day, month, yearStr, today);
        }

        // 6. Check Numeric Date "DD/MM" or "DD-MM"
        Matcher numMatcher = NUMERIC_DATE_PATTERN.matcher(text);
        if (numMatcher.find()) {
            int d = Integer.parseInt(numMatcher.group(1));
            int m = Integer.parseInt(numMatcher.group(2));
            String yearStr = numMatcher.group(3);

            int day = d;
            int month = m;
            // If month > 12 and day <= 12, swap
            if (month > 12 && day <= 12) {
                int temp = day;
                day = month;
                month = temp;
            }

            if (month >= 1 && month <= 12 && day >= 1 && day <= 31) {
                return resolveDateWithYearCheck(day, month, yearStr, today);
            }
        }

        // 7. Check Weekdays ("on monday", "monday", etc.)
        Matcher weekdayMatcher = WEEKDAY_PATTERN.matcher(text);
        if (weekdayMatcher.find()) {
            String dayName = weekdayMatcher.group(1).toUpperCase(Locale.ROOT);
            DayOfWeek targetDay = DayOfWeek.valueOf(dayName);
            DayOfWeek currentDay = today.getDayOfWeek();

            // Most recent past occurrence, NOT today
            int daysAgo = (currentDay.getValue() - targetDay.getValue() + 7) % 7;
            if (daysAgo == 0) {
                daysAgo = 7;
            }
            return new DateParseResult(today.minusDays(daysAgo), false, true);
        }

        // Default: today (not explicit)
        return new DateParseResult(today, false, false);
    }

    private static DateParseResult resolveDateWithYearCheck(int day, int month, String yearStr, LocalDate today) {
        try {
            int year = today.getYear();
            boolean explicitYear = false;
            if (yearStr != null && !yearStr.isBlank()) {
                explicitYear = true;
                year = Integer.parseInt(yearStr);
                if (year < 100) {
                    year += 2000;
                }
            }

            LocalDate candidate = LocalDate.of(year, month, day);

            if (explicitYear) {
                // If an explicit year in the future was supplied, fall back to today
                if (candidate.isAfter(today)) {
                    return new DateParseResult(today, true, true);
                }
                return new DateParseResult(candidate, false, true);
            }

            // If no explicit year was supplied, but candidate is in the future in current year,
            // rule says: "or the previous year if it is in the future."
            if (candidate.isAfter(today)) {
                candidate = candidate.minusYears(1);
            }

            return new DateParseResult(candidate, false, true);
        } catch (Exception e) {
            // Invalid date like 31 Feb -> fall back to today, needsReview
            return new DateParseResult(today, true, true);
        }
    }
}
