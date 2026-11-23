package com.kharcha.parsing.service;

import com.kharcha.common.domain.Category;
import com.kharcha.parsing.ai.LlmCategoryResult;
import com.kharcha.parsing.ai.LlmClient;
import com.kharcha.parsing.dto.ParseExpenseRequest;
import com.kharcha.parsing.dto.ParseExpenseResponse;
import com.kharcha.parsing.engine.AmountParser;
import com.kharcha.parsing.engine.DateParser;
import com.kharcha.parsing.engine.KeywordRuleMatcher;
import com.kharcha.parsing.engine.MerchantExtractor;
import com.kharcha.parsing.engine.ParseConfidence;
import com.kharcha.parsing.engine.ParseMethod;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
public class ExpenseParsingService {

    private final MerchantCorrectionService merchantCorrectionService;
    private final AiUsageService aiUsageService;
    private final LlmClient llmClient;
    private final Clock clock;
    private final boolean aiEnabled;

    @Autowired
    public ExpenseParsingService(
            MerchantCorrectionService merchantCorrectionService,
            AiUsageService aiUsageService,
            LlmClient llmClient,
            Clock clock,
            @Value("${ai.enabled:false}") boolean aiEnabled
    ) {
        this.merchantCorrectionService = merchantCorrectionService;
        this.aiUsageService = aiUsageService;
        this.llmClient = llmClient;
        this.clock = clock;
        this.aiEnabled = aiEnabled;
    }

    // Optional AI integration constructor for unit tests that do not configure application properties.
    public ExpenseParsingService(
            MerchantCorrectionService merchantCorrectionService,
            AiUsageService aiUsageService,
            LlmClient llmClient,
            Clock clock
    ) {
        this(merchantCorrectionService, aiUsageService, llmClient, clock, false);
    }

    public ParseExpenseResponse parseExpense(UUID userId, ParseExpenseRequest request) {
        // 1. Normalise: trim and collapse spaces
        String note = request.text().trim().replaceAll("\\s+", " ");

        // 2. Amount
        Long amountPaise = AmountParser.parseAmountPaise(note);

        // 3. Date
        DateParser.DateParseResult dateResult = DateParser.parseDate(note, clock);
        LocalDate occurredOn = dateResult.date();
        boolean futureFallback = dateResult.isFutureFallback();

        // 4. Keyword match (used for rules & fallback merchant name)
        Optional<KeywordRuleMatcher.KeywordMatch> keywordMatch = KeywordRuleMatcher.matchCategory(note);
        String matchedKeyword = keywordMatch.map(KeywordRuleMatcher.KeywordMatch::matchedKeyword).orElse(null);

        // Merchant extraction from preposition or keyword
        String merchant = MerchantExtractor.extractMerchant(note, matchedKeyword);

        Category category = null;
        ParseMethod method = null;

        // 5. Learned corrections (if this user has corrected this merchant before)
        if (userId != null) {
            if (merchant != null) {
                Optional<Category> learned = merchantCorrectionService.findCorrection(userId, merchant);
                if (learned.isPresent()) {
                    category = learned.get();
                    method = ParseMethod.RULES;
                }
            }
            if (category == null) {
                Optional<com.kharcha.parsing.domain.UserMerchantCategory> matchingCorrection =
                        merchantCorrectionService.findMatchingCorrection(userId, note);
                if (matchingCorrection.isPresent()) {
                    category = matchingCorrection.get().getCategory();
                    method = ParseMethod.RULES;
                    if (merchant == null) {
                        merchant = MerchantExtractor.capitalize(matchingCorrection.get().getMerchantKey());
                    }
                }
            }
        }

        // 6. Keyword rules (merchant or keyword to category)
        if (category == null && keywordMatch.isPresent()) {
            category = keywordMatch.get().category();
            method = ParseMethod.RULES;
        }

        // 7. Optional AI fallback (disabled by default; rules-only parsing always remains available)
        if (aiEnabled && category == null && userId != null) {
            boolean canCall = aiUsageService.canCallAiAndIncrement(userId);
            if (canCall) {
                try {
                    Optional<LlmCategoryResult> aiResult = llmClient.categorize(note);
                    if (aiResult.isPresent()) {
                        LlmCategoryResult res = aiResult.get();
                        if (res.category() != null) {
                            category = res.category();
                            method = ParseMethod.AI;
                        }
                        if (merchant == null && res.merchant() != null && !res.merchant().isBlank()) {
                            merchant = MerchantExtractor.capitalize(res.merchant());
                        }
                    }
                } catch (Exception ignored) {
                    // Failures must never break the request; leave category null and needsReview true
                }
            }
        }

        // 8. Confidence determination
        ParseConfidence confidence;
        if (amountPaise == null || category == null || futureFallback) {
            confidence = ParseConfidence.LOW;
        } else if (method == ParseMethod.RULES) {
            confidence = ParseConfidence.HIGH;
        } else {
            confidence = ParseConfidence.MEDIUM;
        }

        // 9. Needs review determination
        boolean needsReview = (confidence == ParseConfidence.LOW)
                || (amountPaise == null)
                || (category == null)
                || futureFallback;

        return new ParseExpenseResponse(
                amountPaise,
                category != null ? category.name() : null,
                category != null ? category.getDisplayName() : null,
                merchant,
                occurredOn,
                note,
                "TEXT",
                confidence,
                needsReview,
                category != null ? method : null
        );
    }
}
