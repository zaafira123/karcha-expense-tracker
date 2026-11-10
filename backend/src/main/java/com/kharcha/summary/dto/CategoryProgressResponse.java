package com.kharcha.summary.dto;

import com.kharcha.common.domain.Category;
import com.kharcha.summary.domain.BudgetStatus;

public record CategoryProgressResponse(
        Category.CategoryInfo category,
        Long budgetPaise,
        Long spentPaise,
        int percentUsed,
        BudgetStatus status
) {}
