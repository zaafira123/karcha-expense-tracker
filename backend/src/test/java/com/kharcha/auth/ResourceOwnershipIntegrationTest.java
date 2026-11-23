package com.kharcha.auth;

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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResourceOwnershipIntegrationTest extends BaseIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @Autowired TransactionRepository transactions;
    @Autowired BudgetRepository budgets;

    @BeforeEach void clean() { transactions.deleteAll(); budgets.deleteAll(); users.deleteAll(); }

    @Test
    void financialResourcesOnlyReturnTheAuthenticatedUsersRows() throws Exception {
        String first = registerAndLogin("first-owner@example.com");
        String second = registerAndLogin("second-owner@example.com");
        mvc.perform(post("/api/transactions").header("Authorization", "Bearer " + first)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new CreateTransactionRequest(12345L, Category.FOOD_DINING,
                                "Private merchant", LocalDate.of(2026, 11, 10), null))))
                .andExpect(status().isCreated());
        mvc.perform(put("/api/budgets/FOOD_DINING?month=2026-11").header("Authorization", "Bearer " + first)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new UpdateBudgetRequest(50000L))))
                .andExpect(status().isOk());

        mvc.perform(get("/api/transactions?month=2026-11").header("Authorization", "Bearer " + second))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/budgets?month=2026-11").header("Authorization", "Bearer " + second))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.category.code == 'FOOD_DINING')].amountPaise").value(0));
        mvc.perform(get("/api/dashboard?month=2026-11").header("Authorization", "Bearer " + second))
                .andExpect(status().isOk()).andExpect(jsonPath("$.spentPaise").value(0));
        mvc.perform(get("/api/forecast?month=2026-11").header("Authorization", "Bearer " + second))
                .andExpect(status().isOk()).andExpect(jsonPath("$.spentPaise").value(0));
        mvc.perform(get("/api/summary/weekly").header("Authorization", "Bearer " + second))
                .andExpect(status().isOk()).andExpect(jsonPath("$.currentWeekSpentPaise").value(0));
        mvc.perform(get("/api/summary/monthly?month=2026-11").header("Authorization", "Bearer " + second))
                .andExpect(status().isOk()).andExpect(jsonPath("$.currentSpentPaise").value(0))
                .andExpect(jsonPath("$.tips.length()").value(3));
    }

    private String registerAndLogin(String email) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new RegisterRequest(email, "password12345"))))
                .andExpect(status().isCreated());
        JsonNode result = mapper.readTree(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new LoginRequest(email, "password12345"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        return result.path("accessToken").asText();
    }
}
