package com.kharcha.parsing.engine;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MerchantExtractor {

    private MerchantExtractor() {}

    // Prepositions that introduce merchant or item
    private static final Pattern PREPOSITION_PATTERN = Pattern.compile(
            "(?i)\\b(for|at|from|on)\\s+(.+)$"
    );

    // Date indicators to stop extraction
    private static final Pattern DATE_STOP_PATTERN = Pattern.compile(
            "(?i)\\b(today|yesterday|tomorrow|monday|tuesday|wednesday|thursday|friday|saturday|sunday|"
                    + "\\d{1,2}(?:st|nd|rd|th)?\\s+(?:jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)[a-z]*|"
                    + "(?:jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)[a-z]*\\s+\\d{1,2}(?:st|nd|rd|th)?|"
                    + "\\d{1,2}[/-]\\d{1,2}(?:[/-]\\d{2,4})?)\\b"
    );

    // Amount indicators to stop extraction
    private static final Pattern AMOUNT_STOP_PATTERN = Pattern.compile(
            "(?i)\\b(?:₹|rs\\.?|rupees|inr)?\\s*(?:[0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)(?:\\.[0-9]+)?\\s*(?:k|rs\\.?|rupees|inr|₹)?\\b"
    );

    // Check if an "on" is actually introducing a date
    private static final Pattern ON_DATE_PATTERN = Pattern.compile(
            "(?i)^on\\s+(today|yesterday|tomorrow|monday|tuesday|wednesday|thursday|friday|saturday|sunday|"
                    + "\\d{1,2}(?:st|nd|rd|th)?\\s+(?:jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)[a-z]*|"
                    + "(?:jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)[a-z]*\\s+\\d{1,2}(?:st|nd|rd|th)?|"
                    + "\\d{1,2}[/-]\\d{1,2})\\b"
    );

    public static String extractMerchant(String text, String matchedKeyword) {
        if (text == null || text.isBlank()) {
            return capitalize(matchedKeyword);
        }

        String normalized = text.trim();

        // 1. Scan for prepositions "for", "at", "from", "on"
        Pattern p = Pattern.compile("(?i)\\b(for|at|from|on)\\s+");
        Matcher m = p.matcher(normalized);

        while (m.find()) {
            String prep = m.group(1).toLowerCase(Locale.ROOT);
            int startAfterPrep = m.end();
            String candidate = normalized.substring(startAfterPrep).trim();

            // If prep is "on" and it introduces a date (e.g. "on monday", "on 5 nov"), skip it
            if ("on".equals(prep) && ON_DATE_PATTERN.matcher("on " + candidate).find()) {
                continue;
            }

            // Find earliest date or amount word in candidate
            int cutIndex = candidate.length();

            Matcher dateMatcher = DATE_STOP_PATTERN.matcher(candidate);
            if (dateMatcher.find()) {
                cutIndex = Math.min(cutIndex, dateMatcher.start());
            }

            Matcher amountMatcher = AMOUNT_STOP_PATTERN.matcher(candidate);
            if (amountMatcher.find()) {
                cutIndex = Math.min(cutIndex, amountMatcher.start());
            }

            // Also check if followed by another preposition phrase like "on monday"
            Matcher prepMatcher = Pattern.compile("(?i)\\s+\\b(on|at|for|from)\\b\\s+").matcher(candidate);
            if (prepMatcher.find()) {
                cutIndex = Math.min(cutIndex, prepMatcher.start());
            }

            String extracted = candidate.substring(0, cutIndex).trim();
            // Remove any trailing or leading punctuation
            extracted = extracted.replaceAll("^[\\p{Punct}\\s]+|[\\p{Punct}\\s]+$", "").trim();

            if (!extracted.isBlank()) {
                return capitalize(extracted);
            }
        }

        // 2. If no preposition matched, fall back to the matched keyword capitalized, or null
        return capitalize(matchedKeyword);
    }

    public static String capitalize(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }

        String trimmed = s.trim();
        // Capitalize first character
        return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1);
    }
}
