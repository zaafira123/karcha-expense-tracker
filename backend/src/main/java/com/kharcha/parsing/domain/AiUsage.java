package com.kharcha.parsing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "ai_usage",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_ai_usage_date", columnNames = {"user_id", "usage_date"})
)
public class AiUsage {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(name = "calls", nullable = false)
    private int calls;

    public AiUsage() {}

    public AiUsage(UUID id, UUID userId, LocalDate usageDate, int calls) {
        this.id = id;
        this.userId = userId;
        this.usageDate = usageDate;
        this.calls = calls;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public LocalDate getUsageDate() {
        return usageDate;
    }

    public int getCalls() {
        return calls;
    }

    public void incrementCalls() {
        this.calls++;
    }
}
