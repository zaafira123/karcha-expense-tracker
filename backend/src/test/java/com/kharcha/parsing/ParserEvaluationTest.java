package com.kharcha.parsing;

import com.kharcha.common.domain.Category;
import com.kharcha.parsing.ai.LlmCategoryResult;
import com.kharcha.parsing.ai.LlmClient;
import com.kharcha.parsing.dto.ParseExpenseRequest;
import com.kharcha.parsing.dto.ParseExpenseResponse;
import com.kharcha.parsing.repository.AiUsageRepository;
import com.kharcha.parsing.repository.UserMerchantCategoryRepository;
import com.kharcha.parsing.service.AiUsageService;
import com.kharcha.parsing.service.ExpenseParsingService;
import com.kharcha.parsing.service.MerchantCorrectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ParserEvaluationTest {

    private ExpenseParsingService parsingService;
    private LlmClient mockLlmClient;
    private Clock fixedClock;
    private final LocalDate today = LocalDate.of(2026, 11, 10); // Tuesday
    private final UUID testUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        Instant fixedInstant = today.atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant();
        fixedClock = Clock.fixed(fixedInstant, ZoneId.of("Asia/Kolkata"));

        mockLlmClient = Mockito.mock(LlmClient.class);
        UserMerchantCategoryRepository correctionRepo = Mockito.mock(UserMerchantCategoryRepository.class);
        when(correctionRepo.findByUserIdAndMerchantKey(any(), any())).thenReturn(Optional.empty());

        MerchantCorrectionService correctionService = new MerchantCorrectionService(correctionRepo);

        AiUsageRepository aiUsageRepo = Mockito.mock(AiUsageRepository.class);
        when(aiUsageRepo.findByUserIdAndUsageDate(any(), any())).thenReturn(Optional.empty());
        AiUsageService aiUsageService = new AiUsageService(aiUsageRepo, fixedClock, 20);

        // Configure mock AI responses for sentences where rules don't match
        when(mockLlmClient.categorize("dinner with friends at barbeque nation"))
                .thenReturn(Optional.of(new LlmCategoryResult(Category.FOOD_DINING, "Barbeque Nation")));
        when(mockLlmClient.categorize("paid 500 on 25 dec 2026 for gifts"))
                .thenReturn(Optional.of(new LlmCategoryResult(Category.SHOPPING, "Gifts")));
        when(mockLlmClient.categorize("paid 800 at cult fitness"))
                .thenReturn(Optional.of(new LlmCategoryResult(Category.HEALTH, "Cult Fitness")));

        parsingService = new ExpenseParsingService(
                correctionService,
                aiUsageService,
                mockLlmClient,
                fixedClock
        );
    }

    @Test
    @DisplayName("Evaluate 50 sentences and assert accuracy")
    void evaluate50Sentences() throws Exception {
        InputStream is = getClass().getResourceAsStream("/parser-eval.csv");
        assertThat(is).isNotNull();

        int total = 0;
        int correct = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line = reader.readLine(); // header
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                String[] parts = parseCsvLine(line);
                String sentence = parts[0].trim();
                String expectedAmountStr = parts.length > 1 ? parts[1].trim() : "";
                String expectedCatStr = parts.length > 2 ? parts[2].trim() : "";
                String expectedDateStr = parts.length > 3 ? parts[3].trim() : "";

                Long expectedAmount = expectedAmountStr.isBlank() ? null : Long.parseLong(expectedAmountStr);
                Category expectedCategory = expectedCatStr.isBlank() ? null : Category.valueOf(expectedCatStr);

                LocalDate expectedDate = today;
                if ("yesterday".equalsIgnoreCase(expectedDateStr)) {
                    expectedDate = today.minusDays(1);
                } else if ("monday".equalsIgnoreCase(expectedDateStr)) {
                    expectedDate = today.minusDays(1); // yesterday was Monday
                } else if ("5 nov".equalsIgnoreCase(expectedDateStr)) {
                    expectedDate = LocalDate.of(2026, 11, 5);
                }

                ParseExpenseResponse response = parsingService.parseExpense(testUserId, new ParseExpenseRequest(sentence));

                boolean amountMatches = (expectedAmount == null && response.amountPaise() == null)
                        || (expectedAmount != null && expectedAmount.equals(response.amountPaise()));
                boolean categoryMatches = (expectedCategory == null && response.category() == null)
                        || (expectedCategory != null && expectedCategory.name().equals(response.category()));
                boolean dateMatches = expectedDate.equals(response.occurredOn());

                total++;
                if (amountMatches && categoryMatches && dateMatches) {
                    correct++;
                } else {
                    System.out.printf("MISMATCH on line %d: '%s' -> expected [amt=%s, cat=%s, date=%s] got [amt=%s, cat=%s, date=%s]%n",
                            total, sentence, expectedAmount, expectedCategory, expectedDate,
                            response.amountPaise(), response.category(), response.occurredOn());
                }
            }
        }

        System.out.println("Parser accuracy: " + correct + " of " + total);
        assertThat(total).isEqualTo(50);
        assertThat(correct).isGreaterThanOrEqualTo(48);
    }

    private String[] parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim());
        return tokens.toArray(new String[0]);
    }
}
