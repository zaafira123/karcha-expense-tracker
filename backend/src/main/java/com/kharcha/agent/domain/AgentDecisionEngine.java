package com.kharcha.agent.domain;

import com.kharcha.common.domain.Category;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class AgentDecisionEngine {

    private AgentDecisionEngine() {}

    private record ReceiverCandidate(Category category, long shortfall) {}

    private static class DonorState {
        final Category category;
        final long initialSpare;
        long spareRemaining;

        DonorState(Category category, long spare) {
            this.category = category;
            this.initialSpare = spare;
            this.spareRemaining = spare;
        }
    }

    public static List<ProposedMove> generateProposals(
            List<CategoryProjection> projections,
            long capPaise,
            int maxProposals,
            Set<Category> excludedReceivers
    ) {
        if (projections == null || projections.isEmpty() || capPaise <= 0 || maxProposals <= 0) {
            return List.of();
        }

        List<ReceiverCandidate> receivers = new ArrayList<>();
        List<DonorState> donors = new ArrayList<>();

        for (CategoryProjection p : projections) {
            if (p.projectedPaise() == null) {
                continue;
            }

            // 1. Receiver rule: projected > budget
            if (p.projectedPaise() > p.budgetPaise()) {
                if (excludedReceivers == null || !excludedReceivers.contains(p.category())) {
                    long shortfall = p.projectedPaise() - p.budgetPaise();
                    receivers.add(new ReceiverCandidate(p.category(), shortfall));
                }
            }

            // 2. Donor rule: not protected, projected * 100 < budget * 80
            if (!p.isProtected()) {
                if (p.projectedPaise() * 100L < p.budgetPaise() * 80L) {
                    // Ceil(1.25 * projected) = (projected * 5 + 3) / 4
                    long ceil125 = (p.projectedPaise() * 5L + 3L) / 4L;
                    long spare = p.budgetPaise() - ceil125;
                    if (spare > 0L) {
                        donors.add(new DonorState(p.category(), spare));
                    }
                }
            }
        }

        // 3. Sort receivers by shortfall descending, donors by spare descending
        receivers.sort(Comparator.comparingLong(ReceiverCandidate::shortfall).reversed());
        donors.sort(Comparator.comparingLong((DonorState d) -> d.initialSpare).reversed());

        List<ProposedMove> proposals = new ArrayList<>();

        // 4. Pair receivers with donors
        for (ReceiverCandidate receiver : receivers) {
            long shortfallRemaining = receiver.shortfall();

            for (DonorState donor : donors) {
                if (donor.spareRemaining <= 0L) {
                    continue;
                }

                long amount = Math.min(shortfallRemaining, Math.min(donor.spareRemaining, capPaise));
                if (amount > 0L) {
                    proposals.add(new ProposedMove(
                            donor.category,
                            receiver.category(),
                            amount,
                            receiver.shortfall(),
                            donor.initialSpare
                    ));

                    shortfallRemaining -= amount;
                    donor.spareRemaining -= amount;

                    if (proposals.size() == maxProposals) {
                        return proposals;
                    }

                    if (shortfallRemaining <= 0L) {
                        break;
                    }
                }
            }
        }

        return proposals;
    }
}
