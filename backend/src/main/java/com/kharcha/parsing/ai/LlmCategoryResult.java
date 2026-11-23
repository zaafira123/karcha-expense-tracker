package com.kharcha.parsing.ai;

import com.kharcha.common.domain.Category;

public record LlmCategoryResult(
        Category category,
        String merchant
) {}
