package com.kharcha.parsing.domain;

import com.kharcha.common.domain.Category;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "user_merchant_categories",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_merchant", columnNames = {"user_id", "merchant_key"})
)
public class UserMerchantCategory {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "merchant_key", nullable = false, length = 120)
    private String merchantKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 32)
    private Category category;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UserMerchantCategory() {}

    public UserMerchantCategory(UUID id, UUID userId, String merchantKey, Category category, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.merchantKey = merchantKey;
        this.category = category;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getMerchantKey() {
        return merchantKey;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
