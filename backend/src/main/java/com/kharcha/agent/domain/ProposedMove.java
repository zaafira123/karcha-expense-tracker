package com.kharcha.agent.domain;

import com.kharcha.common.domain.Category;

public record ProposedMove(
        Category fromCategory,
        Category toCategory,
        long amountPaise,
        long shortfallPaise,
        long donorSparePaise
) {}
