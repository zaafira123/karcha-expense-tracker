package com.kharcha.agent;

import com.kharcha.agent.domain.AgentDecisionEngine;
import com.kharcha.agent.domain.CategoryProjection;
import com.kharcha.agent.domain.ProposedMove;
import com.kharcha.common.domain.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AgentDecisionEngineTest {

    private static final long DEFAULT_CAP = 200000L; // ₹2,000

    // -------------------------------------------------------------------------
    // Test 1: No shortfall
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("1. No shortfall: all categories within budget returns 0 proposals")
    void testNoShortfall() {
        List<CategoryProjection> projections = List.of(
                new CategoryProjection(Category.FOOD_DINING, 500000L, 400000L, false),
                new CategoryProjection(Category.GROCERIES, 600000L, 300000L, false)
        );

        List<ProposedMove> proposals = AgentDecisionEngine.generateProposals(
                projections, DEFAULT_CAP, 2, Set.of()
        );

        assertThat(proposals).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Test 2: Dining shortfall 1800, Shopping spare 5000, cap 2000 → move 1800
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("2. Dining shortfall 1800, Shopping spare > 1800, cap 2000: move is 1800")
    void testOneMove() {
        // Receiver: Food (budget ₹6,000, projected ₹7,800 → shortfall ₹1,800)
        // Donor: Groceries (budget ₹8,000, projected ₹3,400)
        //   ceil(1.25 × 340000) = (340000×5+3)/4 = 425001/4 = 425000 → spare = 800000-425000 = 375000
        List<CategoryProjection> projections = List.of(
                new CategoryProjection(Category.FOOD_DINING, 600000L, 780000L, false),
                new CategoryProjection(Category.GROCERIES, 800000L, 340000L, false)
        );

        List<ProposedMove> proposals = AgentDecisionEngine.generateProposals(
                projections, DEFAULT_CAP, 2, Set.of()
        );

        assertThat(proposals).hasSize(1);
        ProposedMove move = proposals.get(0);
        assertThat(move.fromCategory()).isEqualTo(Category.GROCERIES);
        assertThat(move.toCategory()).isEqualTo(Category.FOOD_DINING);
        assertThat(move.amountPaise()).isEqualTo(180000L); // ₹1,800
        assertThat(move.shortfallPaise()).isEqualTo(180000L);
        assertThat(move.donorSparePaise()).isEqualTo(375000L);
    }

    // -------------------------------------------------------------------------
    // Test 3: Same but cap 1000 → move capped at 1000
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("3. Same scenario with cap 1000: move is 1000")
    void testCapApplied() {
        // Same receiver shortfall 300000, donor spare 500000, but cap ₹2,000.
        List<CategoryProjection> projections = List.of(
                new CategoryProjection(Category.SHOPPING, 500000L, 800000L, false),
                new CategoryProjection(Category.GROCERIES, 1000000L, 400000L, false)
        );

        List<ProposedMove> proposals = AgentDecisionEngine.generateProposals(
                projections, DEFAULT_CAP, 2, Set.of()
        );

        assertThat(proposals).hasSize(1);
        assertThat(proposals.get(0).amountPaise()).isEqualTo(DEFAULT_CAP); // 200000
    }

    // -------------------------------------------------------------------------
    // Test 4: Donor spare 500 < shortfall 1800 → move is 500
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("4. Donor spare 500, shortfall 1800: move is 500 (spare-limited)")
    void testDonorSpareSmallerThanShortfall() {
        // Donor: budget 500000, projected 304000 → ceil(1.25×304000) = 380000 → spare = 120000
        List<CategoryProjection> projections = List.of(
                new CategoryProjection(Category.FOOD_DINING, 500000L, 700000L, false),
                new CategoryProjection(Category.TRAVEL, 500000L, 304000L, false)
        );

        List<ProposedMove> proposals = AgentDecisionEngine.generateProposals(
                projections, DEFAULT_CAP, 2, Set.of()
        );

        assertThat(proposals).hasSize(1);
        assertThat(proposals.get(0).amountPaise()).isEqualTo(120000L);
    }

    // -------------------------------------------------------------------------
    // Test 5: Savings is the only donor → no proposals (protected)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("5. Savings is the only donor: no proposals (protected category)")
    void testProtectedDonorRejected() {
        List<CategoryProjection> projections = List.of(
                new CategoryProjection(Category.FOOD_DINING, 500000L, 700000L, false),
                new CategoryProjection(Category.BILLS_UTILITIES, 1000000L, 200000L, true),
                new CategoryProjection(Category.HEALTH, 500000L, 50000L, true),
                new CategoryProjection(Category.SAVINGS, 1000000L, 100000L, true)
        );

        List<ProposedMove> proposals = AgentDecisionEngine.generateProposals(
                projections, DEFAULT_CAP, 2, Set.of()
        );

        assertThat(proposals).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Test 6: Bills over budget and Health under: Health can donate, Bills never does
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("6. Bills over budget, Health under budget: Health can donate; Bills cannot donate (protected)")
    void testBillsOverBudgetHealthUnder() {
        List<CategoryProjection> projections = List.of(
                new CategoryProjection(Category.BILLS_UTILITIES, 500000L, 700000L, true),   // over-budget (protected)
                new CategoryProjection(Category.HEALTH, 1000000L, 200000L, true),            // under-budget (protected)
                new CategoryProjection(Category.GROCERIES, 500000L, 700000L, false)           // over-budget receiver
        );

        // Health is protected → cannot donate. Bills is over budget → not eligible donor.
        List<ProposedMove> proposals = AgentDecisionEngine.generateProposals(
                projections, DEFAULT_CAP, 2, Set.of()
        );

        // No unprotected donors → no proposals.
        assertThat(proposals).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Test 7: Three receivers and two donors: at most 2 proposals
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("7. Three receivers, two donors: at most 2 proposals")
    void testTwoProposalLimit() {
        List<CategoryProjection> projections = List.of(
                new CategoryProjection(Category.FOOD_DINING,   500000L, 700000L, false),  // shortfall 200000
                new CategoryProjection(Category.SHOPPING,      300000L, 450000L, false),  // shortfall 150000
                new CategoryProjection(Category.ENTERTAINMENT, 200000L, 300000L, false),  // shortfall 100000
                new CategoryProjection(Category.GROCERIES,    1000000L, 400000L, false),  // spare 500000
                new CategoryProjection(Category.TRAVEL,        800000L, 300000L, false)   // spare ~425000
        );

        List<ProposedMove> proposals = AgentDecisionEngine.generateProposals(
                projections, DEFAULT_CAP, 2, Set.of()
        );

        assertThat(proposals).hasSize(2);
        assertThat(proposals.get(0).toCategory()).isEqualTo(Category.FOOD_DINING);
        assertThat(proposals.get(1).toCategory()).isEqualTo(Category.SHOPPING);
    }

    // -------------------------------------------------------------------------
    // Test 8: Donor at exactly 80% → not a donor
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("8. Donor projected at exactly 80% of budget: not eligible (strict <80%)")
    void testEightyPercentBoundary() {
        // Exactly 80% — rule is projected * 100 < budget * 80  → 80% is NOT eligible.
        List<CategoryProjection> projections = List.of(
                new CategoryProjection(Category.FOOD_DINING, 500000L, 600000L, false),
                new CategoryProjection(Category.GROCERIES, 1000000L, 800000L, false) // exactly 80%
        );

        List<ProposedMove> proposals = AgentDecisionEngine.generateProposals(
                projections, DEFAULT_CAP, 2, Set.of()
        );

        assertThat(proposals).isEmpty();

        // 79.9% → eligible.
        List<CategoryProjection> projectionsUnder80 = List.of(
                new CategoryProjection(Category.FOOD_DINING, 500000L, 600000L, false),
                new CategoryProjection(Category.GROCERIES, 1000000L, 799000L, false)
        );

        List<ProposedMove> proposalsUnder80 = AgentDecisionEngine.generateProposals(
                projectionsUnder80, DEFAULT_CAP, 2, Set.of()
        );

        assertThat(proposalsUnder80).hasSize(1);
    }

    // -------------------------------------------------------------------------
    // Test 9 (property test): After every move, donor satisfies projected ≤ 0.8 × newBudget
    // 1000 random inputs
    // -------------------------------------------------------------------------

    @RepeatedTest(1000)
    @DisplayName("9. Property: after a move, donor projected ≤ 0.8 × new donor budget (1000 random cases)")
    void testDonorSafetyPropertyRandom() {
        Random rng = new Random();

        // Pick random budgets and projections for two categories.
        // Receiver is over budget; donor is under 80%.
        long receiverBudget = 100_000L + (long) (rng.nextInt(900_000));
        long receiverProjected = receiverBudget + 1_000L + (long) (rng.nextInt(500_000));

        long donorBudget = 100_000L + (long) (rng.nextInt(1_000_000));
        // Ensure donor projected < 80% of donor budget.
        long donorProjected = (long) (rng.nextDouble() * 0.799 * donorBudget);

        long cap = 50_000L + (long) (rng.nextInt(500_000));

        List<CategoryProjection> projections = List.of(
                new CategoryProjection(Category.FOOD_DINING, receiverBudget, receiverProjected, false),
                new CategoryProjection(Category.GROCERIES, donorBudget, donorProjected, false)
        );

        List<ProposedMove> proposals = AgentDecisionEngine.generateProposals(
                projections, cap, 2, Set.of()
        );

        for (ProposedMove move : proposals) {
            if (!move.fromCategory().equals(Category.GROCERIES)) continue;

            long newDonorBudget = donorBudget - move.amountPaise();

            // After the move the donor must still satisfy projected ≤ 0.8 × newBudget.
            // Integer equivalent: projected * 100 <= newDonorBudget * 80  (because spare = budget - ceil(1.25*projected))
            assertThat(donorProjected * 100L)
                    .as("After move, donor projected=%d must be ≤ 80%% of new budget=%d",
                            donorProjected, newDonorBudget)
                    .isLessThanOrEqualTo(newDonorBudget * 80L);
        }
    }

    // -------------------------------------------------------------------------
    // Test (existing): Pending duplicate skipped
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Pending duplicate: receiver with existing pending proposal for this month is skipped")
    void testPendingDuplicateSkipped() {
        List<CategoryProjection> projections = List.of(
                new CategoryProjection(Category.FOOD_DINING, 500000L, 700000L, false),
                new CategoryProjection(Category.GROCERIES, 800000L, 300000L, false)
        );

        // FOOD_DINING already has a pending proposal.
        List<ProposedMove> proposals = AgentDecisionEngine.generateProposals(
                projections, DEFAULT_CAP, 2, Set.of(Category.FOOD_DINING)
        );

        assertThat(proposals).isEmpty();
    }
}
