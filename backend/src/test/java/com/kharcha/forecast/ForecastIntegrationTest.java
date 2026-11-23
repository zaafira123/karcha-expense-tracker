package com.kharcha.forecast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kharcha.BaseIntegrationTest;
import com.kharcha.auth.dto.LoginRequest;
import com.kharcha.auth.dto.RegisterRequest;
import com.kharcha.auth.repository.UserRepository;
import com.kharcha.budgets.domain.Budget;
import com.kharcha.budgets.repository.BudgetRepository;
import com.kharcha.common.domain.Category;
import com.kharcha.common.util.MonthUtils;
import com.kharcha.forecast.domain.ForecastStatus;
import com.kharcha.transactions.domain.Transaction;
import com.kharcha.transactions.domain.TransactionSource;
import com.kharcha.transactions.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ForecastIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private Clock clock;

    private String userToken;
    private UUID userId;

    @BeforeEach
    void setUp() throws Exception {
        transactionRepository.deleteAll();
        budgetRepository.deleteAll();
        userRepository.deleteAll();

        userToken = registerAndLogin("forecastuser@example.com", "password123");
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
    @DisplayName("Sufficient history returns calculated forecast and full series")
    void testForecastWithSufficientHistory() throws Exception {
        LocalDate today = LocalDate.now(clock);
        YearMonth currentMonth = YearMonth.from(today);
        String monthStr = MonthUtils.formatMonth(currentMonth);

        // Seed 10 active days of spending in the 30-day window before today (e.g. 100000 paise / day = ₹1,000)
        for (int i = 1; i <= 10; i++) {
            LocalDate pastDate = today.minusDays(i);
            Transaction tx = new Transaction(
                    UUID.randomUUID(),
                    userId,
                    100000L,
                    Category.FOOD_DINING,
                    "Restaurant " + i,
                    pastDate,
                    "History spend",
                    TransactionSource.MANUAL,
                    null,
                    Instant.now(clock)
            );
            transactionRepository.save(tx);
        }

        // Set budget for FOOD_DINING of 5000000 paise (₹50,000)
        Budget budget = new Budget(UUID.randomUUID(), userId, Category.FOOD_DINING, monthStr, 5000000L);
        budgetRepository.save(budget);

        mockMvc.perform(get("/api/forecast?month=" + monthStr)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value(monthStr))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.projectedPaise").isNumber())
                .andExpect(jsonPath("$.lowPaise").isNumber())
                .andExpect(jsonPath("$.highPaise").isNumber())
                .andExpect(jsonPath("$.budgetPaise").value(5000000))
                .andExpect(jsonPath("$.series").isArray())
                .andExpect(jsonPath("$.series.length()").value(currentMonth.lengthOfMonth()))
                .andExpect(jsonPath("$.categories.length()").value(8));
    }

    @Test
    @DisplayName("Fewer than 7 active days returns INSUFFICIENT_DATA and null projections")
    void testForecastWithInsufficientData() throws Exception {
        LocalDate today = LocalDate.now(clock);
        YearMonth currentMonth = YearMonth.from(today);
        String monthStr = MonthUtils.formatMonth(currentMonth);

        // Seed only 3 days of spending in the 30-day window
        for (int i = 1; i <= 3; i++) {
            LocalDate pastDate = today.minusDays(i);
            Transaction tx = new Transaction(
                    UUID.randomUUID(),
                    userId,
                    50000L,
                    Category.GROCERIES,
                    "Grocery " + i,
                    pastDate,
                    "Few days spend",
                    TransactionSource.MANUAL,
                    null,
                    Instant.now(clock)
            );
            transactionRepository.save(tx);
        }

        mockMvc.perform(get("/api/forecast?month=" + monthStr)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ForecastStatus.INSUFFICIENT_DATA.name()))
                .andExpect(jsonPath("$.projectedPaise").doesNotExist())
                .andExpect(jsonPath("$.lowPaise").doesNotExist())
                .andExpect(jsonPath("$.highPaise").doesNotExist())
                .andExpect(jsonPath("$.avgDailyPaise").doesNotExist());
    }
}
