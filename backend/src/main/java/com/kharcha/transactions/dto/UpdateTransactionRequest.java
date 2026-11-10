package com.kharcha.transactions.dto;

import com.kharcha.common.domain.Category;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateTransactionRequest(
        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than 0")
        Long amountPaise,

        @NotNull(message = "Category is required")
        Category category,

        @Size(max = 120, message = "Merchant cannot exceed 120 characters")
        String merchant,

        @NotNull(message = "Date is required")
        LocalDate occurredOn,

        @Size(max = 255, message = "Note cannot exceed 255 characters")
        String note
) {}
