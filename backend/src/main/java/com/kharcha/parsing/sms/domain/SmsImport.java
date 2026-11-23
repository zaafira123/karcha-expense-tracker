package com.kharcha.parsing.sms.domain;

import com.kharcha.parsing.sms.engine.SmsMessageType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "sms_imports")
public class SmsImport {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "message_hash", nullable = false, length = 64, updatable = false)
    private String messageHash;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private SmsMessageType type;

    @Column(name = "amount_paise", nullable = false, updatable = false)
    private Long amountPaise;

    @Column(name = "occurred_on", nullable = false, updatable = false)
    private LocalDate occurredOn;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SmsImport() {}

    public SmsImport(UUID id, UUID userId, String messageHash, UUID transactionId, SmsMessageType type,
                     Long amountPaise, LocalDate occurredOn, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.messageHash = messageHash;
        this.transactionId = transactionId;
        this.type = type;
        this.amountPaise = amountPaise;
        this.occurredOn = occurredOn;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getMessageHash() { return messageHash; }
    public UUID getTransactionId() { return transactionId; }
    public SmsMessageType getType() { return type; }
    public Long getAmountPaise() { return amountPaise; }
    public LocalDate getOccurredOn() { return occurredOn; }
    public Instant getCreatedAt() { return createdAt; }
}
