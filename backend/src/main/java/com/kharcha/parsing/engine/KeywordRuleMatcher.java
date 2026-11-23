package com.kharcha.parsing.engine;

import com.kharcha.common.domain.Category;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class KeywordRuleMatcher {

    private KeywordRuleMatcher() {}

    public record KeywordMatch(Category category, String matchedKeyword) {}

    private record Rule(Category category, String keyword, Pattern pattern) {}

    private static final List<Rule> RULES;

    static {
        List<RuleEntry> entries = List.of(
                // FOOD_DINING
                new RuleEntry(Category.FOOD_DINING, "swiggy"),
                new RuleEntry(Category.FOOD_DINING, "zomato"),
                new RuleEntry(Category.FOOD_DINING, "dominos"),
                new RuleEntry(Category.FOOD_DINING, "pizza"),
                new RuleEntry(Category.FOOD_DINING, "restaurant"),
                new RuleEntry(Category.FOOD_DINING, "cafe"),
                new RuleEntry(Category.FOOD_DINING, "starbucks"),
                new RuleEntry(Category.FOOD_DINING, "food"),

                // GROCERIES
                new RuleEntry(Category.GROCERIES, "bigbasket"),
                new RuleEntry(Category.GROCERIES, "blinkit"),
                new RuleEntry(Category.GROCERIES, "zepto"),
                new RuleEntry(Category.GROCERIES, "dmart"),
                new RuleEntry(Category.GROCERIES, "big bazaar"),
                new RuleEntry(Category.GROCERIES, "grocery"),
                new RuleEntry(Category.GROCERIES, "groceries"),
                new RuleEntry(Category.GROCERIES, "vegetables"),

                // TRAVEL
                new RuleEntry(Category.TRAVEL, "uber"),
                new RuleEntry(Category.TRAVEL, "ola"),
                new RuleEntry(Category.TRAVEL, "rapido"),
                new RuleEntry(Category.TRAVEL, "metro"),
                new RuleEntry(Category.TRAVEL, "irctc"),
                new RuleEntry(Category.TRAVEL, "petrol"),
                new RuleEntry(Category.TRAVEL, "fuel"),
                new RuleEntry(Category.TRAVEL, "fastag"),
                new RuleEntry(Category.TRAVEL, "flight"),
                new RuleEntry(Category.TRAVEL, "train"),

                // SHOPPING
                new RuleEntry(Category.SHOPPING, "amazon"),
                new RuleEntry(Category.SHOPPING, "flipkart"),
                new RuleEntry(Category.SHOPPING, "myntra"),
                new RuleEntry(Category.SHOPPING, "ajio"),
                new RuleEntry(Category.SHOPPING, "shopping"),
                new RuleEntry(Category.SHOPPING, "clothes"),

                // BILLS_UTILITIES
                new RuleEntry(Category.BILLS_UTILITIES, "electricity"),
                new RuleEntry(Category.BILLS_UTILITIES, "water bill"),
                new RuleEntry(Category.BILLS_UTILITIES, "gas bill"),
                new RuleEntry(Category.BILLS_UTILITIES, "broadband"),
                new RuleEntry(Category.BILLS_UTILITIES, "airtel"),
                new RuleEntry(Category.BILLS_UTILITIES, "jio"),
                new RuleEntry(Category.BILLS_UTILITIES, "rent"),
                new RuleEntry(Category.BILLS_UTILITIES, "maintenance"),
                new RuleEntry(Category.BILLS_UTILITIES, "insurance premium"),

                // HEALTH
                new RuleEntry(Category.HEALTH, "pharmacy"),
                new RuleEntry(Category.HEALTH, "medplus"),
                new RuleEntry(Category.HEALTH, "apollo"),
                new RuleEntry(Category.HEALTH, "doctor"),
                new RuleEntry(Category.HEALTH, "medicine"),
                new RuleEntry(Category.HEALTH, "hospital"),
                new RuleEntry(Category.HEALTH, "clinic"),

                // ENTERTAINMENT
                new RuleEntry(Category.ENTERTAINMENT, "netflix"),
                new RuleEntry(Category.ENTERTAINMENT, "spotify"),
                new RuleEntry(Category.ENTERTAINMENT, "prime video"),
                new RuleEntry(Category.ENTERTAINMENT, "movie"),
                new RuleEntry(Category.ENTERTAINMENT, "bookmyshow"),
                new RuleEntry(Category.ENTERTAINMENT, "game"),
                new RuleEntry(Category.ENTERTAINMENT, "concert"),

                // SAVINGS
                new RuleEntry(Category.SAVINGS, "savings"),
                new RuleEntry(Category.SAVINGS, "fd"),
                new RuleEntry(Category.SAVINGS, "mutual fund"),
                new RuleEntry(Category.SAVINGS, "sip")
        );

        // Sort rules so longer phrases (e.g. "mutual fund", "water bill") match before shorter words
        RULES = entries.stream()
                .sorted(Comparator.comparingInt((RuleEntry r) -> r.keyword().length()).reversed())
                .map(r -> new Rule(
                        r.category(),
                        r.keyword(),
                        Pattern.compile("(?i)\\b" + Pattern.quote(r.keyword()) + "\\b")
                ))
                .toList();
    }

    private record RuleEntry(Category category, String keyword) {}

    public static Optional<KeywordMatch> matchCategory(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        for (Rule rule : RULES) {
            Matcher m = rule.pattern().matcher(text);
            if (m.find()) {
                return Optional.of(new KeywordMatch(rule.category(), rule.keyword()));
            }
        }

        return Optional.empty();
    }
}
