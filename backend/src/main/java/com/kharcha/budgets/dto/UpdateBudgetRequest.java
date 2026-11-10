package com.kharcha.budgets.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateBudgetRequest(
        @NotNull(message = "Amount is required")
        @Min(value = 0, message = "Amount must be 0 or more")
        Long amountPaise
) {}
