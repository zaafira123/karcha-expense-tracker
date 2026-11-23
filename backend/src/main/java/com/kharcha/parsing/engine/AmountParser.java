package com.kharcha.parsing.engine;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AmountParser {

    private AmountParser() {}

    // Currency symbols / words
    private static final String CURRENCY_PREFIX = "(?:₹|rs\\.?|rupees|inr)";
    private static final String CURRENCY_SUFFIX = "(?:rs\\.?|rupees|inr|₹)";

    // Date patterns to avoid capturing as amounts (e.g., "5 nov", "nov 5", "5/11", "5-11")
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(?i)\\b(?:\\d{1,2}(?:st|nd|rd|th)?\\s+(?:jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)[a-z]*"
                    + "|(?:jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)[a-z]*\\s+\\d{1,2}(?:st|nd|rd|th)?"
                    + "|\\d{1,2}[/-]\\d{1,2}(?:[/-]\\d{2,4})?)\\b"
    );

    // Explicit currency followed by amount: e.g. "rs 450", "₹1,200", "inr 1.5k"
    private static final Pattern EXPLICIT_CURRENCY_PREFIX = Pattern.compile(
            "(?i)" + CURRENCY_PREFIX + "\\s*([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)(?:\\.([0-9]+))?\\s*(k)?\\b"
    );

    // Amount followed by currency: e.g. "450 rs", "450rupees", "1.5k inr"
    private static final Pattern EXPLICIT_CURRENCY_SUFFIX = Pattern.compile(
            "(?i)\\b([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)(?:\\.([0-9]+))?\\s*(k)?\\s*" + CURRENCY_SUFFIX
    );

    // General number pattern: e.g. "450", "1,200", "1200.50", "1.5k"
    private static final Pattern GENERAL_NUMBER = Pattern.compile(
            "(?i)\\b([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)(?:\\.([0-9]+))?\\s*(k)?\\b"
    );

    public static Long parseAmountPaise(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return null;
        }

        String text = rawText.trim();

        // 1. Try explicit prefix pattern first
        Matcher prefixMatcher = EXPLICIT_CURRENCY_PREFIX.matcher(text);
        if (prefixMatcher.find()) {
            return convertToPaise(prefixMatcher.group(1), prefixMatcher.group(2), prefixMatcher.group(3));
        }

        // 2. Try explicit suffix pattern
        Matcher suffixMatcher = EXPLICIT_CURRENCY_SUFFIX.matcher(text);
        if (suffixMatcher.find()) {
            return convertToPaise(suffixMatcher.group(1), suffixMatcher.group(2), suffixMatcher.group(3));
        }

        // 3. Find general number, skipping those that overlap with date patterns
        // Find date ranges to exclude
        Matcher dateMatcher = DATE_PATTERN.matcher(text);
        java.util.List<int[]> dateRanges = new java.util.ArrayList<>();
        while (dateMatcher.find()) {
            dateRanges.add(new int[]{dateMatcher.start(), dateMatcher.end()});
        }

        Matcher generalMatcher = GENERAL_NUMBER.matcher(text);
        while (generalMatcher.find()) {
            int start = generalMatcher.start();
            int end = generalMatcher.end();

            boolean overlapsDate = false;
            for (int[] range : dateRanges) {
                if (start >= range[0] && end <= range[1]) {
                    overlapsDate = true;
                    break;
                }
            }

            if (!overlapsDate) {
                return convertToPaise(generalMatcher.group(1), generalMatcher.group(2), generalMatcher.group(3));
            }
        }

        return null;
    }

    private static Long convertToPaise(String wholePartStr, String decimalPartStr, String kSuffix) {
        if (wholePartStr == null) {
            return null;
        }

        try {
            String cleanWhole = wholePartStr.replace(",", "");
            long whole = Long.parseLong(cleanWhole);
            boolean isK = kSuffix != null && !kSuffix.isBlank();

            if (isK) {
                // E.g., 1.5k -> 1500 rupees = 150000 paise
                // whole * 1000 rupees * 100 paise = whole * 100000 paise
                long paise = whole * 100_000L;
                if (decimalPartStr != null && !decimalPartStr.isBlank()) {
                    // Up to 3 decimal places for thousands (e.g. .5 -> 500, .25 -> 250, .125 -> 125)
                    String decimal3 = (decimalPartStr + "000").substring(0, 3);
                    long decimalRupees = Long.parseLong(decimal3);
                    paise += decimalRupees * 100L;
                }
                return paise > 0 ? paise : null;
            } else {
                // Normal rupees to paise: whole * 100
                long paise = whole * 100L;
                if (decimalPartStr != null && !decimalPartStr.isBlank()) {
                    String decimal2 = (decimalPartStr + "00").substring(0, 2);
                    paise += Long.parseLong(decimal2);
                }
                return paise > 0 ? paise : null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
