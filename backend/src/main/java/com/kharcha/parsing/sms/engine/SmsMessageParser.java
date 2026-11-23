package com.kharcha.parsing.sms.engine;

import com.kharcha.common.domain.Category;
import com.kharcha.parsing.engine.AmountParser;
import com.kharcha.parsing.engine.DateParser;
import com.kharcha.parsing.engine.KeywordRuleMatcher;
import com.kharcha.parsing.engine.MerchantExtractor;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure SMS transaction parser. It never persists or logs the input message. */
public final class SmsMessageParser {

    private static final Pattern DEBIT = Pattern.compile("(?i)\\b(debited|debit|spent|purchase(?:d)?|paid|sent|withdrawn|payment)\\b");
    private static final Pattern CREDIT = Pattern.compile("(?i)\\b(credited|credit|received|refund(?:ed)?|reversed|cashback)\\b");
    private static final Pattern VPA = Pattern.compile("(?i)\\bVPA\\s*[:=-]?\\s*([\\p{L}\\p{N}._-]+@[\\p{L}\\p{N}._-]+)");
    private static final Pattern MERCHANT_AFTER = Pattern.compile("(?i)\\b(?:to|at)\\s+(.+)$");
    private static final Pattern MERCHANT_STOP = Pattern.compile("(?i)\\b(?:on|ref(?:erence)?(?:\\s*(?:no|number))?|upi ref|txn(?:\\s*id)?|transaction id|using|a/c|account|successful|was|from)\\b");
    private static final Pattern COMPACT_DATE = Pattern.compile("(?i)\\b(\\d{1,2})(jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)(\\d{2,4})\\b");
    private static final Pattern REFERENCE = Pattern.compile("(?i)\\b(?:upi\\s*)?ref(?:erence)?(?:\\s*(?:no|number))?\\s*[:#-]?\\s*([a-z0-9-]{4,})");

    private SmsMessageParser() {}

    public static SmsMessageDraft parse(String message, Clock clock) {
        LocalDate today = LocalDate.now(clock);
        if (message == null || message.isBlank()) return review(SmsMessageType.UNKNOWN, null, null, today, null, "Message is empty.");

        String normalizedDateMessage = normalizeCompactDate(message);
        Long amountPaise = AmountParser.parseAmountPaise(normalizedDateMessage);
        SmsMessageType type = classify(normalizedDateMessage);
        DateParser.DateParseResult dateResult = DateParser.parseDate(normalizedDateMessage, clock);
        LocalDate occurredOn = dateResult.date();
        String merchant = extractMerchant(normalizedDateMessage);
        String referenceHint = extractReference(normalizedDateMessage);
        Category category = KeywordRuleMatcher.matchCategory(merchant).map(KeywordRuleMatcher.KeywordMatch::category).orElse(null);

        boolean futureDate = dateResult.isFutureFallback();
        String reason = null;
        if (type == SmsMessageType.UNKNOWN) reason = "Could not tell whether this was a debit or credit.";
        else if (amountPaise == null) reason = "Could not read a clear amount.";
        else if (futureDate) reason = "The message contains a future or invalid date.";
        else if (type == SmsMessageType.DEBIT && merchant == null) reason = "Could not read a clear merchant.";
        else if (type == SmsMessageType.DEBIT && category == null) reason = "Choose a category before saving.";

        boolean needsReview = reason != null;
        String confidence = needsReview ? "LOW" : (dateResult.isExplicitDate() ? "HIGH" : "MEDIUM");
        return new SmsMessageDraft(type, amountPaise, merchant, occurredOn, referenceHint, confidence, category, needsReview, reason);
    }

    private static SmsMessageDraft review(SmsMessageType type, Long amount, String merchant, LocalDate date, String ref, String reason) {
        return new SmsMessageDraft(type, amount, merchant, date, ref, "LOW", null, true, reason);
    }

    private static SmsMessageType classify(String text) {
        Matcher debit = DEBIT.matcher(text);
        Matcher credit = CREDIT.matcher(text);
        boolean hasDebit = debit.find();
        boolean hasCredit = credit.find();
        if (!hasDebit && !hasCredit) return SmsMessageType.UNKNOWN;
        if (!hasDebit) return SmsMessageType.CREDIT;
        if (!hasCredit) return SmsMessageType.DEBIT;
        return debit.start() <= credit.start() ? SmsMessageType.DEBIT : SmsMessageType.CREDIT;
    }

    private static String normalizeCompactDate(String message) {
        Matcher matcher = COMPACT_DATE.matcher(message);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String year = matcher.group(3);
            if (year.length() == 2) year = "20" + year;
            matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group(1) + " " + matcher.group(2) + " " + year));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String extractMerchant(String message) {
        Matcher vpa = VPA.matcher(message);
        if (vpa.find()) return titleCase(vpa.group(1).substring(0, vpa.group(1).indexOf('@')).replaceAll("[._-]+", " "));

        Matcher after = MERCHANT_AFTER.matcher(message);
        if (!after.find()) return null;
        String candidate = after.group(1);
        Matcher stop = MERCHANT_STOP.matcher(candidate);
        if (stop.find()) candidate = candidate.substring(0, stop.start());
        candidate = candidate.replaceAll("^[\\p{Punct}\\s]+|[\\p{Punct}\\s]+$", "").trim();
        if (candidate.isBlank()) return null;
        if (candidate.matches("(?i)(your|account|credit card|a/c|debit card|card)")) return null;

        String extracted = MerchantExtractor.extractMerchant("paid at " + candidate, null);
        return extracted == null ? null : titleCase(extracted);
    }

    private static String titleCase(String value) {
        if (value == null || value.isBlank()) return null;
        String[] words = value.trim().replaceAll("\\s+", " ").split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) continue;
            if (result.length() > 0) result.append(' ');
            String lower = word.toLowerCase(Locale.ROOT);
            result.append(Character.toUpperCase(lower.charAt(0))).append(lower.substring(1));
        }
        return result.length() == 0 ? null : result.toString();
    }

    private static String extractReference(String message) {
        Matcher matcher = REFERENCE.matcher(message);
        return matcher.find() ? matcher.group(1) : null;
    }
}
