package com.kharcha.agent.domain;

import com.kharcha.common.domain.Category;

public record CategoryProjection(
        Category category,
        long budgetPaise,
        Long projectedPaise,
        boolean isProtected
) {}
