package com.kharcha.agent.event;

import com.kharcha.common.domain.Category;

import java.util.UUID;

/**
 * Published when a transaction pushes a category's spend to ≥ 80% of its monthly budget.
 * The agent listener reacts by running proposals for that user and month.
 */
public record CategoryThresholdEvent(
        UUID userId,
        Category category,
        String month
) {}
