package com.kharcha.transactions.dto;

import com.kharcha.common.domain.Category;
import com.kharcha.transactions.domain.TransactionSource;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateTransactionRequest(
        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than 0")
        Long amountPaise,

        @NotNull(message = "Category is required")
        Category category,

        @Size(max = 120, message = "Merchant cannot exceed 120 characters")
        String merchant,

        LocalDate occurredOn,

        @Size(max = 255, message = "Note cannot exceed 255 characters")
        String note,

        TransactionSource source
) {
    public CreateTransactionRequest(Long amountPaise, Category category, String merchant,
                                    LocalDate occurredOn, String note) {
        this(amountPaise, category, merchant, occurredOn, note, TransactionSource.MANUAL);
    }
}
