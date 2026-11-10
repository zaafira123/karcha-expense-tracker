package com.kharcha.budgets;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BudgetIntegrationTest extends BaseIntegrationTest {

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

        RegisterRequest register = new RegisterRequest("budgetuser@example.com", "password123");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest("budgetuser@example.com", "password123");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        userToken = json.get("accessToken").asText();
    }

    @Test
    @DisplayName("GET /api/budgets returns all eight categories even when unset")
    void testGetBudgetsReturnsAllEightCategories() throws Exception {
        mockMvc.perform(get("/api/budgets?month=2026-10")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8))
                .andExpect(jsonPath("$[0].amountPaise").value(0))
                .andExpect(jsonPath("$[0].spentPaise").value(0));
    }

    @Test
    @DisplayName("Setting a budget creates it, setting it twice updates it")
    void testSetAndUpdateBudget() throws Exception {
        // Set budget to ₹6,000 (600000 paise)
        UpdateBudgetRequest setRequest = new UpdateBudgetRequest(600000L);
        mockMvc.perform(put("/api/budgets/FOOD_DINING?month=2026-10")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(setRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category.code").value("FOOD_DINING"))
                .andExpect(jsonPath("$.amountPaise").value(600000));

        // Update same budget to ₹7,500 (750000 paise)
        UpdateBudgetRequest updateRequest = new UpdateBudgetRequest(750000L);
        mockMvc.perform(put("/api/budgets/FOOD_DINING?month=2026-10")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category.code").value("FOOD_DINING"))
                .andExpect(jsonPath("$.amountPaise").value(750000));

        // List budgets: exactly one row for FOOD_DINING, still 8 categories total
        MvcResult listResult = mockMvc.perform(get("/api/budgets?month=2026-10")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8))
                .andReturn();

        JsonNode listNode = objectMapper.readTree(listResult.getResponse().getContentAsString());
        boolean foundFood = false;
        for (JsonNode item : listNode) {
            if ("FOOD_DINING".equals(item.get("category").get("code").asText())) {
                assertThat(item.get("amountPaise").asLong()).isEqualTo(750000L);
                foundFood = true;
            }
        }
        assertThat(foundFood).isTrue();
    }

    @Test
    @DisplayName("Budgets reflect accurate spent amounts from transactions in that month")
    void testBudgetsReflectSpentAmounts() throws Exception {
        // Add expense of ₹4,200 in FOOD_DINING on 2026-10-10
        CreateTransactionRequest tx = new CreateTransactionRequest(
                420000L, Category.FOOD_DINING, "Restaurant", LocalDate.of(2026, 10, 10), null
        );
        mockMvc.perform(post("/api/transactions")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tx)));

        // Set budget for FOOD_DINING to ₹6,000
        UpdateBudgetRequest budgetReq = new UpdateBudgetRequest(600000L);
        mockMvc.perform(put("/api/budgets/FOOD_DINING?month=2026-10")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(budgetReq)));

        // Query budgets
        mockMvc.perform(get("/api/budgets?month=2026-10")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.category.code == 'FOOD_DINING')].amountPaise").value(600000))
                .andExpect(jsonPath("$[?(@.category.code == 'FOOD_DINING')].spentPaise").value(420000));
    }
}
