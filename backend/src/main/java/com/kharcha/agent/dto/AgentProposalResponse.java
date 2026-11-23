package com.kharcha.agent.dto;

import com.kharcha.agent.domain.AgentProposal;
import com.kharcha.agent.domain.ProposalStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Agent suggested budget rebalancing proposal")
public record AgentProposalResponse(
        @Schema(description = "Unique proposal ID")
        UUID id,

        @Schema(description = "Target month in YYYY-MM format", example = "2026-10")
        String month,

        @Schema(description = "Donor category code (gives funds)", example = "GROCERIES")
        String fromCategory,

        @Schema(description = "Donor category user-facing display name", example = "Groceries")
        String fromCategoryName,

        @Schema(description = "Receiver category code (receives funds)", example = "FOOD_DINING")
        String toCategory,

        @Schema(description = "Receiver category user-facing display name", example = "Food and dining")
        String toCategoryName,

        @Schema(description = "Suggested transfer amount in paise", example = "180000")
        long amountPaise,

        @Schema(description = "Shortfall of receiver category in paise", example = "180000")
        long shortfallPaise,

        @Schema(description = "Donor spare room in paise", example = "375000")
        long donorSparePaise,

        @Schema(description = "Proposal status", example = "PENDING")
        ProposalStatus status,

        @Schema(description = "Creation timestamp")
        Instant createdAt,

        @Schema(description = "Decision timestamp (when approved or rejected)")
        Instant decidedAt,

        @Schema(description = "Application timestamp (when applied to budgets)")
        Instant appliedAt,

        @Schema(description = "Expiration timestamp (7 days after creation)")
        Instant expiresAt
) {
    public static AgentProposalResponse from(AgentProposal proposal) {
        return new AgentProposalResponse(
                proposal.getId(),
                proposal.getMonth(),
                proposal.getFromCategory().name(),
                proposal.getFromCategory().getDisplayName(),
                proposal.getToCategory().name(),
                proposal.getToCategory().getDisplayName(),
                proposal.getAmountPaise(),
                proposal.getShortfallPaise(),
                proposal.getDonorSparePaise(),
                proposal.getStatus(),
                proposal.getCreatedAt(),
                proposal.getDecidedAt(),
                proposal.getAppliedAt(),
                proposal.getExpiresAt()
        );
    }
}
