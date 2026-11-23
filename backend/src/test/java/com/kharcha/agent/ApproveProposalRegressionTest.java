package com.kharcha.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kharcha.BaseIntegrationTest;
import com.kharcha.agent.domain.AgentAuditEventType;
import com.kharcha.agent.dto.AgentProposalResponse;
import com.kharcha.agent.repository.AgentAuditRepository;
import com.kharcha.agent.repository.AgentProposalRepository;
import com.kharcha.auth.dto.LoginRequest;
import com.kharcha.auth.dto.RegisterRequest;
import com.kharcha.auth.repository.UserRepository;
import com.kharcha.budgets.domain.Budget;
import com.kharcha.budgets.repository.BudgetRepository;
import com.kharcha.common.domain.Category;
import com.kharcha.common.util.MonthUtils;
import com.kharcha.transactions.domain.Transaction;
import com.kharcha.transactions.domain.TransactionSource;
import com.kharcha.transactions.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApproveProposalRegressionTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private BudgetRepository budgetRepository;
    @Autowired private AgentAuditRepository auditRepository;
    @Autowired private AgentProposalRepository proposalRepository;
    @Autowired private Clock clock;

    @BeforeEach
    void clearData() {
        auditRepository.deleteAll();
        proposalRepository.deleteAll();
        budgetRepository.deleteAll();
        transactionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void approvesPendingProposalWithoutResponseBodyAndRejectsSecondApproval() throws Exception {
        String email = "approve-regression-" + UUID.randomUUID() + "@example.com";
        String password = "password123";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(email, password))))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("accessToken").asText();
        UUID userId = userRepository.findByEmail(email).orElseThrow().getId();
        String month = MonthUtils.formatMonth(YearMonth.from(LocalDate.now(clock)));

        for (int daysAgo = 1; daysAgo <= 10; daysAgo++) {
            transactionRepository.save(new Transaction(
                    UUID.randomUUID(), userId, 50000L, Category.FOOD_DINING,
                    "Regression test merchant " + daysAgo, LocalDate.now(clock).minusDays(daysAgo),
                    null, TransactionSource.MANUAL, null, Instant.now(clock)
            ));
        }

        long donorBefore = 1000000L;
        long receiverBefore = 200000L;
        budgetRepository.save(new Budget(UUID.randomUUID(), userId, Category.GROCERIES, month, donorBefore));
        budgetRepository.save(new Budget(UUID.randomUUID(), userId, Category.FOOD_DINING, month, receiverBefore));

        mockMvc.perform(post("/api/agent/proposals/run")
                        .param("month", month)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        MvcResult pendingResult = mockMvc.perform(get("/api/agent/proposals")
                        .param("status", "PENDING")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        AgentProposalResponse[] pending = objectMapper.readValue(
                pendingResult.getResponse().getContentAsString(), AgentProposalResponse[].class);
        assertThat(pending).hasSize(1);
        AgentProposalResponse proposal = pending[0];
        assertThat(proposal.fromCategory()).isEqualTo(Category.GROCERIES.name());
        assertThat(proposal.toCategory()).isEqualTo(Category.FOOD_DINING.name());

        mockMvc.perform(post("/api/agent/proposals/" + proposal.id() + "/approve")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        long donorAfter = budgetRepository
                .findByUserIdAndCategoryAndMonth(userId, Category.GROCERIES, month)
                .orElseThrow().getAmountPaise();
        long receiverAfter = budgetRepository
                .findByUserIdAndCategoryAndMonth(userId, Category.FOOD_DINING, month)
                .orElseThrow().getAmountPaise();
        assertThat(donorAfter).isEqualTo(donorBefore - proposal.amountPaise());
        assertThat(receiverAfter).isEqualTo(receiverBefore + proposal.amountPaise());
        assertThat(donorAfter + receiverAfter).isEqualTo(donorBefore + receiverBefore);

        assertThat(auditRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .anySatisfy(audit -> {
                    assertThat(audit.getProposalId()).isEqualTo(proposal.id());
                    assertThat(audit.getEvent()).isEqualTo(AgentAuditEventType.APPLIED);
                });

        mockMvc.perform(post("/api/agent/proposals/" + proposal.id() + "/approve")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }
}
