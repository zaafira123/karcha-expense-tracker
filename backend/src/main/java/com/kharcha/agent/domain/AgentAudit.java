package com.kharcha.agent.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent_audit")
public class AgentAudit {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "proposal_id")
    private UUID proposalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event", nullable = false, length = 20)
    private AgentAuditEventType event;

    @Column(name = "detail", columnDefinition = "TEXT")
    private String detail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public AgentAudit() {}

    public AgentAudit(UUID id, UUID userId, UUID proposalId, AgentAuditEventType event, String detail, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.proposalId = proposalId;
        this.event = event;
        this.detail = detail;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getProposalId() {
        return proposalId;
    }

    public AgentAuditEventType getEvent() {
        return event;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
