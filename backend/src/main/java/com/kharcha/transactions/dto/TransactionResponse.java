package com.kharcha.transactions.dto;

import com.kharcha.common.domain.Category;
import com.kharcha.transactions.domain.Transaction;
import com.kharcha.transactions.domain.TransactionSource;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        Long amountPaise,
        Category.CategoryInfo category,
        String merchant,
        LocalDate occurredOn,
        String note,
        TransactionSource source,
        Instant createdAt
) {
    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getAmountPaise(),
                t.getCategory().toInfo(),
                t.getMerchant(),
                t.getOccurredOn(),
                t.getNote(),
                t.getSource(),
                t.getCreatedAt()
        );
    }
}
