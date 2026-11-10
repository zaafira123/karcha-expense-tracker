package com.kharcha.budgets.dto;

import com.kharcha.common.domain.Category;

public record CategoryBudgetResponse(
        Category.CategoryInfo category,
        Long amountPaise,
        Long spentPaise
) {}
