package com.kharcha.budgets.dto;

import java.util.List;

public record BudgetListResponse(
        String month,
        List<CategoryBudgetResponse> categories
) {}
