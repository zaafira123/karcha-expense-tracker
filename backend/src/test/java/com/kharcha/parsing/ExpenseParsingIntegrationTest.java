package com.kharcha.parsing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kharcha.BaseIntegrationTest;
import com.kharcha.auth.dto.LoginRequest;
import com.kharcha.auth.dto.RegisterRequest;
import com.kharcha.auth.repository.UserRepository;
import com.kharcha.common.domain.Category;
import com.kharcha.parsing.ai.LlmCategoryResult;
import com.kharcha.parsing.ai.LlmClient;
import com.kharcha.parsing.domain.AiUsage;
import com.kharcha.parsing.dto.ParseExpenseRequest;
import com.kharcha.parsing.repository.AiUsageRepository;
import com.kharcha.parsing.repository.UserMerchantCategoryRepository;
import com.kharcha.transactions.dto.CreateTransactionRequest;
import com.kharcha.transactions.dto.TransactionResponse;
import com.kharcha.transactions.dto.UpdateTransactionRequest;
import com.kharcha.transactions.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExpenseParsingIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserMerchantCategoryRepository userMerchantCategoryRepository;

    @Autowired
    private AiUsageRepository aiUsageRepository;

    @Autowired
    private Clock clock;

    @MockBean
    private LlmClient mockLlmClient;

    private String userToken;
    private UUID userId;

    @BeforeEach
    void setUp() throws Exception {
        transactionRepository.deleteAll();
        userMerchantCategoryRepository.deleteAll();
        aiUsageRepository.deleteAll();
        userRepository.deleteAll();

        userToken = registerAndLogin("parseruser@example.com", "password123");
    }

    private String registerAndLogin(String email, String password) throws Exception {
        RegisterRequest register = new RegisterRequest(email, password);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest(email, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        this.userId = userRepository.findByEmail(email).orElseThrow().getId();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("accessToken").asText();
    }

    @Test
    @DisplayName("Parse returns a draft and saves nothing to the database")
    void testParseReturnsDraftAndSavesNothing() throws Exception {
        ParseExpenseRequest request = new ParseExpenseRequest("paid 450 for pizza with friends");

        mockMvc.perform(post("/api/expenses/parse")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amountPaise").value(45000))
                .andExpect(jsonPath("$.category").value("FOOD_DINING"))
                .andExpect(jsonPath("$.categoryName").value("Food and dining"))
                .andExpect(jsonPath("$.merchant").value("Pizza with friends"))
                .andExpect(jsonPath("$.confidence").value("HIGH"))
                .andExpect(jsonPath("$.needsReview").value(false))
                .andExpect(jsonPath("$.method").value("RULES"));

        assertThat(transactionRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("Corrections are saved when transaction is edited and reused in next parse")
    void testCorrectionsSavedAndReusedInNextParse() throws Exception {
        // 1. Create a transaction with an unknown merchant and initial category
        CreateTransactionRequest createReq = new CreateTransactionRequest(
                80000L,
                Category.TRAVEL,
                "Cult Fitness",
                LocalDate.now(clock),
                "Initial note"
        );

        MvcResult createResult = mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        TransactionResponse createdTx = objectMapper.readValue(createResult.getResponse().getContentAsString(), TransactionResponse.class);

        // 2. Edit transaction to HEALTH category (user correction)
        UpdateTransactionRequest updateReq = new UpdateTransactionRequest(
                80000L,
                Category.HEALTH,
                "Cult Fitness",
                LocalDate.now(clock),
                "Corrected note"
        );

        mockMvc.perform(put("/api/transactions/" + createdTx.id())
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk());

        // 3. Next parse for "cult fitness 800" should use learned correction (method RULES)
        ParseExpenseRequest parseReq = new ParseExpenseRequest("cult fitness 800");

        mockMvc.perform(post("/api/expenses/parse")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(parseReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amountPaise").value(80000))
                .andExpect(jsonPath("$.category").value("HEALTH"))
                .andExpect(jsonPath("$.categoryName").value("Health"))
                .andExpect(jsonPath("$.merchant").value("Cult fitness"))
                .andExpect(jsonPath("$.method").value("RULES"))
                .andExpect(jsonPath("$.confidence").value("HIGH"))
                .andExpect(jsonPath("$.needsReview").value(false));
    }

    @Test
    @DisplayName("AI failure returns 200 with needsReview true and category null")
    void testAiFailureReturns200WithNeedsReviewTrue() throws Exception {
        when(mockLlmClient.categorize(any())).thenThrow(new RuntimeException("AI API timeout"));

        ParseExpenseRequest parseReq = new ParseExpenseRequest("guitar lessons 1500");

        mockMvc.perform(post("/api/expenses/parse")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(parseReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amountPaise").value(150000))
                .andExpect(jsonPath("$.category").doesNotExist())
                .andExpect(jsonPath("$.confidence").value("LOW"))
                .andExpect(jsonPath("$.needsReview").value(true));
    }

    @Test
    @DisplayName("Daily AI limit reached skips AI call and returns needsReview true")
    void testDailyAiLimitReachedSkipsAi() throws Exception {
        // Pre-fill ai_usage with 20 calls (default limit)
        LocalDate today = LocalDate.now(clock);
        aiUsageRepository.save(new AiUsage(UUID.randomUUID(), userId, today, 20));

        ParseExpenseRequest parseReq = new ParseExpenseRequest("custom course fee 3000");

        mockMvc.perform(post("/api/expenses/parse")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(parseReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amountPaise").value(300000))
                .andExpect(jsonPath("$.category").doesNotExist())
                .andExpect(jsonPath("$.confidence").value("LOW"))
                .andExpect(jsonPath("$.needsReview").value(true));

        verify(mockLlmClient, never()).categorize(any());
    }

    @Test
    @DisplayName("AI returning null or invalid category is safely ignored")
    void testAiInvalidCategoryIgnored() throws Exception {
        when(mockLlmClient.categorize(any()))
                .thenReturn(Optional.of(new LlmCategoryResult(null, "Unknown Merchant")));

        ParseExpenseRequest parseReq = new ParseExpenseRequest("special service 500");

        mockMvc.perform(post("/api/expenses/parse")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(parseReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amountPaise").value(50000))
                .andExpect(jsonPath("$.category").doesNotExist())
                .andExpect(jsonPath("$.confidence").value("LOW"))
                .andExpect(jsonPath("$.needsReview").value(true));
    }
}
