package com.kharcha.agent.domain;

import com.kharcha.common.domain.Category;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent_proposals")
public class AgentProposal {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @JdbcTypeCode(Types.CHAR)
    @Column(name = "month", nullable = false, length = 7, columnDefinition = "char(7)")
    private String month;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_category", nullable = false, length = 32)
    private Category fromCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_category", nullable = false, length = 32)
    private Category toCategory;

    @Column(name = "amount_paise", nullable = false)
    private long amountPaise;

    @Column(name = "shortfall_paise", nullable = false)
    private long shortfallPaise;

    @Column(name = "donor_spare_paise", nullable = false)
    private long donorSparePaise;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 12)
    private ProposalStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "applied_at")
    private Instant appliedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "donor_budget_before_paise")
    private Long donorBudgetBeforePaise;

    @Column(name = "receiver_budget_before_paise")
    private Long receiverBudgetBeforePaise;

    public AgentProposal() {}

    public AgentProposal(
            UUID id,
            UUID userId,
            String month,
            Category fromCategory,
            Category toCategory,
            long amountPaise,
            long shortfallPaise,
            long donorSparePaise,
            ProposalStatus status,
            Instant createdAt,
            Instant decidedAt,
            Instant appliedAt,
            Instant expiresAt
    ) {
        this.id = id;
        this.userId = userId;
        this.month = month;
        this.fromCategory = fromCategory;
        this.toCategory = toCategory;
        this.amountPaise = amountPaise;
        this.shortfallPaise = shortfallPaise;
        this.donorSparePaise = donorSparePaise;
        this.status = status;
        this.createdAt = createdAt;
        this.decidedAt = decidedAt;
        this.appliedAt = appliedAt;
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getMonth() {
        return month;
    }

    public Category getFromCategory() {
        return fromCategory;
    }

    public Category getToCategory() {
        return toCategory;
    }

    public long getAmountPaise() {
        return amountPaise;
    }

    public long getShortfallPaise() {
        return shortfallPaise;
    }

    public long getDonorSparePaise() {
        return donorSparePaise;
    }

    public ProposalStatus getStatus() {
        return status;
    }

    public void setStatus(ProposalStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(Instant decidedAt) {
        this.decidedAt = decidedAt;
    }

    public Instant getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(Instant appliedAt) {
        this.appliedAt = appliedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Long getDonorBudgetBeforePaise() {
        return donorBudgetBeforePaise;
    }

    public void setDonorBudgetBeforePaise(Long donorBudgetBeforePaise) {
        this.donorBudgetBeforePaise = donorBudgetBeforePaise;
    }

    public Long getReceiverBudgetBeforePaise() {
        return receiverBudgetBeforePaise;
    }

    public void setReceiverBudgetBeforePaise(Long receiverBudgetBeforePaise) {
        this.receiverBudgetBeforePaise = receiverBudgetBeforePaise;
    }
}
