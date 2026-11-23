package com.kharcha.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kharcha.BaseIntegrationTest;
import com.kharcha.agent.domain.AgentProposal;
import com.kharcha.agent.domain.ProposalStatus;
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
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AgentIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private BudgetRepository budgetRepository;
    @Autowired private AgentProposalRepository proposalRepository;
    @Autowired private AgentAuditRepository auditRepository;
    @Autowired private Clock clock;

    private String userAToken;
    private UUID userAId;
    private String userBToken;
    private UUID userBId;

    @BeforeEach
    void setUp() throws Exception {
        auditRepository.deleteAll();
        proposalRepository.deleteAll();
        budgetRepository.deleteAll();
        transactionRepository.deleteAll();
        userRepository.deleteAll();

        userAToken = registerAndLogin("usera@agent.com", "password123");
        userAId = userRepository.findByEmail("usera@agent.com").orElseThrow().getId();

        userBToken = registerAndLogin("userb@agent.com", "password123");
        userBId = userRepository.findByEmail("userb@agent.com").orElseThrow().getId();
    }

    private String registerAndLogin(String email, String password) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(email, password))))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).path("accessToken").asText();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String currentMonth() {
        return MonthUtils.formatMonth(YearMonth.from(LocalDate.now(clock)));
    }

    /** Seeds n days of history for the given category so forecast status = OK. */
    private void seedHistory(UUID userId, Category category, int days) {
        LocalDate today = LocalDate.now(clock);
        for (int i = 1; i <= days; i++) {
            transactionRepository.save(new Transaction(
                    UUID.randomUUID(), userId, 50000L, category,
                    "Merchant " + i, today.minusDays(i), null, TransactionSource.MANUAL, null, Instant.now(clock)
            ));
        }
    }

    private AgentProposal pendingProposal(UUID userId, String month,
                                          Category from, Category to, long amount) {
        Instant now = Instant.now(clock);
        AgentProposal p = new AgentProposal(
                UUID.randomUUID(), userId, month, from, to,
                amount, amount, amount * 2, ProposalStatus.PENDING,
                now, null, null, now.plus(7, ChronoUnit.DAYS)
        );
        return proposalRepository.save(p);
    }

    private AgentProposal appliedProposal(UUID userId, String month,
                                          Category from, Category to, long amount,
                                          Instant appliedAt) {
        Instant now = Instant.now(clock);
        AgentProposal p = new AgentProposal(
                UUID.randomUUID(), userId, month, from, to,
                amount, amount, amount * 2, ProposalStatus.APPLIED,
                now, appliedAt, appliedAt, now.plus(7, ChronoUnit.DAYS)
        );
        p.setDonorBudgetBeforePaise(500000L);
        p.setReceiverBudgetBeforePaise(200000L);
        return proposalRepository.save(p);
    }

    // =========================================================================
    // Test 1: Full end-to-end flow — run, approve, undo
    // =========================================================================

    @Test
    @DisplayName("1. End-to-end: run proposals, approve move, undo within 7 days")
    void testEndToEndAgentFlow() throws Exception {
        String month = currentMonth();
        seedHistory(userAId, Category.FOOD_DINING, 10);

        budgetRepository.save(new Budget(UUID.randomUUID(), userAId, Category.FOOD_DINING, month, 200000L));
        budgetRepository.save(new Budget(UUID.randomUUID(), userAId, Category.GROCERIES, month, 1000000L));

        MvcResult runResult = mockMvc.perform(post("/api/agent/proposals/run?month=" + month)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andReturn();

        AgentProposalResponse[] proposals = objectMapper.readValue(
                runResult.getResponse().getContentAsString(), AgentProposalResponse[].class);

        assertThat(proposals).isNotEmpty();
        AgentProposalResponse proposal = proposals[0];
        assertThat(proposal.status()).isEqualTo(ProposalStatus.PENDING);
        assertThat(proposal.fromCategory()).isEqualTo("GROCERIES");
        assertThat(proposal.toCategory()).isEqualTo("FOOD_DINING");
        assertThat(proposal.amountPaise()).isGreaterThan(0L);
        assertThat(auditRepository.findByUserIdOrderByCreatedAtDesc(userAId)).isNotEmpty();

        // Approve
        mockMvc.perform(post("/api/agent/proposals/" + proposal.id() + "/approve")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPLIED"));

        Budget donor = budgetRepository.findByUserIdAndCategoryAndMonth(userAId, Category.GROCERIES, month).orElseThrow();
        Budget receiver = budgetRepository.findByUserIdAndCategoryAndMonth(userAId, Category.FOOD_DINING, month).orElseThrow();
        assertThat(donor.getAmountPaise()).isEqualTo(1000000L - proposal.amountPaise());
        assertThat(receiver.getAmountPaise()).isEqualTo(200000L + proposal.amountPaise());

        // Undo — returns UndoProposalResponse
        mockMvc.perform(post("/api/agent/proposals/" + proposal.id() + "/undo")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proposal.status").value("UNDONE"))
                .andExpect(jsonPath("$.budgetsChangedSinceApply").value(false));

        Budget restoredDonor = budgetRepository.findByUserIdAndCategoryAndMonth(userAId, Category.GROCERIES, month).orElseThrow();
        Budget restoredReceiver = budgetRepository.findByUserIdAndCategoryAndMonth(userAId, Category.FOOD_DINING, month).orElseThrow();
        assertThat(restoredDonor.getAmountPaise()).isEqualTo(1000000L);
        assertThat(restoredReceiver.getAmountPaise()).isEqualTo(200000L);
    }

    // =========================================================================
    // Test 2: Reject
    // =========================================================================

    @Test
    @DisplayName("2. Rejecting a proposal sets REJECTED and leaves budgets unchanged")
    void testRejectProposal() throws Exception {
        String month = currentMonth();
        budgetRepository.save(new Budget(UUID.randomUUID(), userAId, Category.GROCERIES, month, 500000L));
        budgetRepository.save(new Budget(UUID.randomUUID(), userAId, Category.FOOD_DINING, month, 100000L));

        AgentProposal p = pendingProposal(userAId, month, Category.GROCERIES, Category.FOOD_DINING, 50000L);

        mockMvc.perform(post("/api/agent/proposals/" + p.getId() + "/reject")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        assertThat(budgetRepository.findByUserIdAndCategoryAndMonth(userAId, Category.GROCERIES, month)
                .orElseThrow().getAmountPaise()).isEqualTo(500000L);
    }

    // =========================================================================
    // Test 3: Cross-user isolation
    // =========================================================================

    @Test
    @DisplayName("3. User B cannot approve User A's proposal — returns 404")
    void testCrossUserOwnershipIsolation() throws Exception {
        String month = currentMonth();
        AgentProposal p = pendingProposal(userAId, month, Category.GROCERIES, Category.FOOD_DINING, 50000L);

        mockMvc.perform(post("/api/agent/proposals/" + p.getId() + "/approve")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // Test 4: Expired proposal → 409 PROPOSAL_EXPIRED
    // =========================================================================

    @Test
    @DisplayName("4. Expired proposal cannot be approved — returns 409 PROPOSAL_EXPIRED")
    void testExpiredProposalCannotBeApproved() throws Exception {
        String month = currentMonth();
        Instant past = Instant.now(clock).minus(8, ChronoUnit.DAYS);
        Instant expiredAt = Instant.now(clock).minus(1, ChronoUnit.DAYS);

        AgentProposal p = new AgentProposal(
                UUID.randomUUID(), userAId, month,
                Category.GROCERIES, Category.FOOD_DINING,
                50000L, 80000L, 100000L, ProposalStatus.PENDING,
                past, null, null, expiredAt
        );
        proposalRepository.save(p);

        mockMvc.perform(post("/api/agent/proposals/" + p.getId() + "/approve")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("PROPOSAL_EXPIRED"));
    }

    // =========================================================================
    // Test 5: Undo outside 7-day window → 409 UNDO_WINDOW_CLOSED
    // =========================================================================

    @Test
    @DisplayName("5. Undo after 7-day window → 409 UNDO_WINDOW_CLOSED")
    void testUndoWindowClosed() throws Exception {
        String month = currentMonth();
        budgetRepository.save(new Budget(UUID.randomUUID(), userAId, Category.GROCERIES, month, 450000L));
        budgetRepository.save(new Budget(UUID.randomUUID(), userAId, Category.FOOD_DINING, month, 250000L));

        Instant appliedAt = Instant.now(clock).minus(8, ChronoUnit.DAYS);
        AgentProposal p = appliedProposal(userAId, month, Category.GROCERIES, Category.FOOD_DINING, 50000L, appliedAt);

        mockMvc.perform(post("/api/agent/proposals/" + p.getId() + "/undo")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("UNDO_WINDOW_CLOSED"));
    }

    // =========================================================================
    // Test 6: Double-approve → 409 CONFLICT
    // =========================================================================

    @Test
    @DisplayName("6. Approving an already-APPLIED proposal → 409 CONFLICT")
    void testDoubleApprove() throws Exception {
        String month = currentMonth();
        budgetRepository.save(new Budget(UUID.randomUUID(), userAId, Category.GROCERIES, month, 450000L));
        budgetRepository.save(new Budget(UUID.randomUUID(), userAId, Category.FOOD_DINING, month, 250000L));

        AgentProposal p = appliedProposal(userAId, month, Category.GROCERIES, Category.FOOD_DINING, 50000L, Instant.now(clock));

        mockMvc.perform(post("/api/agent/proposals/" + p.getId() + "/approve")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    // =========================================================================
    // Test 7: Total budget is invariant
    // =========================================================================

    @Test
    @DisplayName("7. Total budget (donor + receiver) unchanged after approve and after undo")
    void testTotalBudgetInvariant() throws Exception {
        String month = currentMonth();
        seedHistory(userAId, Category.FOOD_DINING, 10);

        long donorInitial    = 1000000L;
        long receiverInitial = 200000L;
        long totalInitial    = donorInitial + receiverInitial;

        budgetRepository.save(new Budget(UUID.randomUUID(), userAId, Category.FOOD_DINING, month, receiverInitial));
        budgetRepository.save(new Budget(UUID.randomUUID(), userAId, Category.GROCERIES, month, donorInitial));

        MvcResult runResult = mockMvc.perform(post("/api/agent/proposals/run?month=" + month)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andReturn();

        AgentProposalResponse[] proposals = objectMapper.readValue(
                runResult.getResponse().getContentAsString(), AgentProposalResponse[].class);
        assertThat(proposals).isNotEmpty();
        UUID proposalId = proposals[0].id();

        mockMvc.perform(post("/api/agent/proposals/" + proposalId + "/approve")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk());

        long afterApproveDonor    = budgetRepository.findByUserIdAndCategoryAndMonth(userAId, Category.GROCERIES, month).orElseThrow().getAmountPaise();
        long afterApproveReceiver = budgetRepository.findByUserIdAndCategoryAndMonth(userAId, Category.FOOD_DINING, month).orElseThrow().getAmountPaise();
        assertThat(afterApproveDonor + afterApproveReceiver).isEqualTo(totalInitial);

        mockMvc.perform(post("/api/agent/proposals/" + proposalId + "/undo")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk());

        long afterUndoDonor    = budgetRepository.findByUserIdAndCategoryAndMonth(userAId, Category.GROCERIES, month).orElseThrow().getAmountPaise();
        long afterUndoReceiver = budgetRepository.findByUserIdAndCategoryAndMonth(userAId, Category.FOOD_DINING, month).orElseThrow().getAmountPaise();
        assertThat(afterUndoDonor + afterUndoReceiver).isEqualTo(totalInitial);
    }

    // =========================================================================
    // Test 8: /why endpoint
    // =========================================================================

    @Test
    @DisplayName("8. GET /api/agent/proposals/{id}/why returns a template explanation")
    void testGetProposalWhy() throws Exception {
        String month = currentMonth();
        AgentProposal p = pendingProposal(userAId, month, Category.GROCERIES, Category.FOOD_DINING, 100000L);

        mockMvc.perform(get("/api/agent/proposals/" + p.getId() + "/why")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proposalId").value(p.getId().toString()))
                .andExpect(jsonPath("$.explanation").isString())
                .andExpect(jsonPath("$.explanation").isNotEmpty());
    }

    // =========================================================================
    // Test 9: budgetsChangedSinceApply = true
    // =========================================================================

    @Test
    @DisplayName("9. budgetsChangedSinceApply is true when budgets drifted between apply and undo")
    void testBudgetsChangedSinceApplyDetected() throws Exception {
        String month = currentMonth();
        // Snapshots are donor_before=500000, receiver_before=200000, amount=50000
        // So expected after apply: donor=450000, receiver=250000
        // We seed receiver at 350000 (drifted) → budgetsChangedSinceApply = true
        budgetRepository.save(new Budget(UUID.randomUUID(), userAId, Category.GROCERIES, month, 450000L));
        budgetRepository.save(new Budget(UUID.randomUUID(), userAId, Category.FOOD_DINING, month, 350000L));

        Instant appliedAt = Instant.now(clock).minus(1, ChronoUnit.HOURS);
        AgentProposal p = appliedProposal(userAId, month, Category.GROCERIES, Category.FOOD_DINING, 50000L, appliedAt);

        mockMvc.perform(post("/api/agent/proposals/" + p.getId() + "/undo")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proposal.status").value("UNDONE"))
                .andExpect(jsonPath("$.budgetsChangedSinceApply").value(true));
    }

    // =========================================================================
    // Test 10: Pending duplicate skipped
    // =========================================================================

    @Test
    @DisplayName("10. runProposalsFor skips a category that already has a PENDING proposal")
    void testPendingDuplicateSkipped() throws Exception {
        String month = currentMonth();
        seedHistory(userAId, Category.FOOD_DINING, 10);

        budgetRepository.save(new Budget(UUID.randomUUID(), userAId, Category.FOOD_DINING, month, 200000L));
        budgetRepository.save(new Budget(UUID.randomUUID(), userAId, Category.GROCERIES, month, 1000000L));

        // Pre-create a PENDING proposal for FOOD_DINING (the receiver that would be generated)
        pendingProposal(userAId, month, Category.GROCERIES, Category.FOOD_DINING, 50000L);

        MvcResult runResult = mockMvc.perform(post("/api/agent/proposals/run?month=" + month)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andReturn();

        AgentProposalResponse[] proposals = objectMapper.readValue(
                runResult.getResponse().getContentAsString(), AgentProposalResponse[].class);

        for (AgentProposalResponse pr : proposals) {
            assertThat(pr.toCategory()).isNotEqualTo("FOOD_DINING");
        }
    }

    // =========================================================================
    // Test 11: INSUFFICIENT_DATA → empty list
    // =========================================================================

    @Test
    @DisplayName("11. runProposalsFor returns empty list when forecast is INSUFFICIENT_DATA")
    void testInsufficientDataReturnsEmpty() throws Exception {
        String month = currentMonth();
        seedHistory(userAId, Category.FOOD_DINING, 3); // only 3 days, need 7
        budgetRepository.save(new Budget(UUID.randomUUID(), userAId, Category.FOOD_DINING, month, 200000L));

        MvcResult runResult = mockMvc.perform(post("/api/agent/proposals/run?month=" + month)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andReturn();

        AgentProposalResponse[] proposals = objectMapper.readValue(
                runResult.getResponse().getContentAsString(), AgentProposalResponse[].class);

        assertThat(proposals).isEmpty();
    }

    // =========================================================================
    // Test 12: Audit log has PROPOSED, APPLIED, UNDONE in order
    // =========================================================================

    @Test
    @DisplayName("12. Audit log contains PROPOSED, APPLIED, UNDONE events in correct order")
    void testAuditLogEntries() throws Exception {
        String month = currentMonth();
        seedHistory(userAId, Category.FOOD_DINING, 10);

        budgetRepository.save(new Budget(UUID.randomUUID(), userAId, Category.FOOD_DINING, month, 200000L));
        budgetRepository.save(new Budget(UUID.randomUUID(), userAId, Category.GROCERIES, month, 1000000L));

        MvcResult runResult = mockMvc.perform(post("/api/agent/proposals/run?month=" + month)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andReturn();

        UUID proposalId = objectMapper.readValue(
                runResult.getResponse().getContentAsString(), AgentProposalResponse[].class)[0].id();

        mockMvc.perform(post("/api/agent/proposals/" + proposalId + "/approve")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/agent/proposals/" + proposalId + "/undo")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk());

        MvcResult auditResult = mockMvc.perform(get("/api/agent/audit")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode auditArray = objectMapper.readTree(auditResult.getResponse().getContentAsString());
        // Newest first: UNDONE, APPLIED, PROPOSED
        assertThat(auditArray.get(0).path("event").asText()).isEqualTo("UNDONE");
        assertThat(auditArray.get(1).path("event").asText()).isEqualTo("APPLIED");
        assertThat(auditArray.get(2).path("event").asText()).isEqualTo("PROPOSED");
    }
}
