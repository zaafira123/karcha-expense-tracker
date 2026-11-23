package com.kharcha.summary.dto;

import java.util.List;

public record MonthlySummaryResponse(
        String month,
        long currentSpentPaise,
        long previousSpentPaise,
        long changePaise,
        List<CategoryTotal> topCategories,
        List<String> tips,
        String summaryText
) {
    public record CategoryTotal(String category, String categoryName, long amountPaise) {}
}
