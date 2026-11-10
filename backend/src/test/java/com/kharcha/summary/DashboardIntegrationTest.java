package com.kharcha.summary;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kharcha.BaseIntegrationTest;
import com.kharcha.auth.dto.LoginRequest;
import com.kharcha.auth.dto.RegisterRequest;
import com.kharcha.auth.repository.UserRepository;
import com.kharcha.budgets.dto.UpdateBudgetRequest;
import com.kharcha.budgets.repository.BudgetRepository;
import com.kharcha.common.domain.Category;
import com.kharcha.transactions.dto.CreateTransactionRequest;
import com.kharcha.transactions.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        budgetRepository.deleteAll();
        transactionRepository.deleteAll();
        userRepository.deleteAll();

        RegisterRequest register = new RegisterRequest("dashuser@example.com", "password123");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest("dashuser@example.com", "password123");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        userToken = json.get("accessToken").asText();
    }

    @Test
    @DisplayName("Empty dashboard returns zeros, OK statuses, and empty recent transactions")
    void testEmptyDashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard?month=2026-10")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBudgetPaise").value(0))
                .andExpect(jsonPath("$.spentPaise").value(0))
                .andExpect(jsonPath("$.remainingPaise").value(0))
                .andExpect(jsonPath("$.daysLeft").isNumber())
                .andExpect(jsonPath("$.categories.length()").value(8))
                .andExpect(jsonPath("$.recent.length()").value(0));
    }

    @Test
    @DisplayName("Dashboard aggregates totals, categories status, and recent transactions correctly")
    void testDashboardWithData() throws Exception {
        // Budget: Food ₹6,000 (600000 paise), Groceries ₹8,000 (800000 paise) -> Total: ₹14,000 (1400000 paise)
        mockMvc.perform(put("/api/budgets/FOOD_DINING?month=2026-10")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateBudgetRequest(600000L))));

        mockMvc.perform(put("/api/budgets/GROCERIES?month=2026-10")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateBudgetRequest(800000L))));

        // Expenses:
        // Food: ₹4,200 (420000 paise) -> 70% (OK)
        mockMvc.perform(post("/api/transactions")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateTransactionRequest(
                        420000L, Category.FOOD_DINING, "Restaurant", LocalDate.of(2026, 10, 5), null
                ))));

        // Groceries: ₹3,400 (340000 paise) -> 42% (OK)
        mockMvc.perform(post("/api/transactions")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateTransactionRequest(
                        340000L, Category.GROCERIES, "Supermarket", LocalDate.of(2026, 10, 8), null
                ))));

        // Total Spent: 420000 + 340000 = 760000 paise (₹7,600)
        // Remaining: 1400000 - 760000 = 640000 paise (₹6,400)
        mockMvc.perform(get("/api/dashboard?month=2026-10")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBudgetPaise").value(1400000))
                .andExpect(jsonPath("$.spentPaise").value(760000))
                .andExpect(jsonPath("$.remainingPaise").value(640000))
                .andExpect(jsonPath("$.categories[?(@.category.code == 'FOOD_DINING')].percentUsed").value(70))
                .andExpect(jsonPath("$.categories[?(@.category.code == 'FOOD_DINING')].status").value("OK"))
                .andExpect(jsonPath("$.recent.length()").value(2))
                .andExpect(jsonPath("$.recent[0].merchant").value("Supermarket"));
    }

    @Test
    @DisplayName("Invalid month format returns 400 Bad Request")
    void testInvalidMonthFormatReturns400() throws Exception {
        mockMvc.perform(get("/api/dashboard?month=invalid-month")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }
}
