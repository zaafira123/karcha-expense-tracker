package com.kharcha.summary.dto;

import java.time.LocalDate;
import java.util.List;

public record WeeklySummaryResponse(
        LocalDate currentWeekStart,
        LocalDate currentWeekEnd,
        long currentWeekSpentPaise,
        long previousWeekSpentPaise,
        long changePaise,
        List<CategoryChange> categoryIncreases,
        List<CategoryTotal> topCategories,
        List<BudgetAlert> budgetsOver80,
        List<String> tips,
        String summaryText
) {
    public record CategoryChange(String category, String categoryName, long changePaise) {}
    public record CategoryTotal(String category, String categoryName, long amountPaise) {}
    public record BudgetAlert(String category, String categoryName, long spentPaise, long budgetPaise, int percentUsed) {}
}
