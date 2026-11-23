package com.kharcha.summary.domain;

import com.kharcha.common.domain.Category;
import com.kharcha.summary.dto.WeeklySummaryResponse;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.text.NumberFormat;
import java.util.Locale;

public final class WeeklySummaryCalculator {
    private WeeklySummaryCalculator() {}

    public static WeeklySummaryResponse calculate(LocalDate start, LocalDate end,
                                                  Map<Category, Long> current,
                                                  Map<Category, Long> previous,
                                                  Map<Category, Long> monthSpend,
                                                  Map<Category, Long> budgets) {
        Map<Category, Long> currentValues = values(current);
        Map<Category, Long> previousValues = values(previous);
        long currentTotal = currentValues.values().stream().mapToLong(Long::longValue).sum();
        long previousTotal = previousValues.values().stream().mapToLong(Long::longValue).sum();

        List<WeeklySummaryResponse.CategoryChange> increases = java.util.Arrays.stream(Category.values())
                .map(category -> new WeeklySummaryResponse.CategoryChange(category.name(), category.getDisplayName(),
                        currentValues.get(category) - previousValues.get(category)))
                .filter(change -> change.changePaise() > 0)
                .sorted(Comparator.comparingLong(WeeklySummaryResponse.CategoryChange::changePaise).reversed())
                .toList();
        List<WeeklySummaryResponse.CategoryTotal> top = java.util.Arrays.stream(Category.values())
                .filter(category -> currentValues.get(category) > 0)
                .map(category -> new WeeklySummaryResponse.CategoryTotal(category.name(), category.getDisplayName(), currentValues.get(category)))
                .sorted(Comparator.comparingLong(WeeklySummaryResponse.CategoryTotal::amountPaise).reversed())
                .limit(3).toList();
        List<WeeklySummaryResponse.BudgetAlert> alerts = java.util.Arrays.stream(Category.values())
                .filter(category -> budgets.getOrDefault(category, 0L) > 0)
                .filter(category -> monthSpend.getOrDefault(category, 0L) * 100L >= budgets.get(category) * 80L)
                .map(category -> {
                    long spent = monthSpend.getOrDefault(category, 0L);
                    long budget = budgets.get(category);
                    return new WeeklySummaryResponse.BudgetAlert(category.name(), category.getDisplayName(), spent, budget,
                            (int) Math.min(100L, (spent * 100L) / budget));
                }).sorted(Comparator.comparingInt(WeeklySummaryResponse.BudgetAlert::percentUsed).reversed()).toList();
        String summary = currentTotal == 0
                ? "You recorded no expenses in the last 7 days."
                : "You spent " + money(currentTotal) + " in the last 7 days, "
                + (currentTotal >= previousTotal ? "up" : "down") + " " + money(Math.abs(currentTotal - previousTotal))
                + " compared with the previous 7 days.";
        List<String> tips = List.of(
                top.isEmpty() ? "Keep recording expenses to see your top spending categories." : "Review your recent spending in " + top.get(0).categoryName() + ".",
                alerts.isEmpty() ? "Check your category budgets before planning larger purchases." : "Review " + alerts.get(0).categoryName() + ", which is nearing its monthly limit.",
                increases.isEmpty() ? "Keep checking your weekly spending to spot changes early." : "Check the recent increase in " + increases.get(0).categoryName() + "."
        );
        return new WeeklySummaryResponse(start, end, currentTotal, previousTotal, currentTotal - previousTotal,
                increases, top, alerts, tips, summary);
    }

    private static Map<Category, Long> values(Map<Category, Long> source) {
        Map<Category, Long> result = new EnumMap<>(Category.class);
        for (Category category : Category.values()) result.put(category, source.getOrDefault(category, 0L));
        return result;
    }

    private static String money(long paise) {
        long rupees = paise / 100;
        long remainder = Math.abs(paise % 100);
        String grouped = NumberFormat.getIntegerInstance(new Locale("en", "IN")).format(rupees);
        return "₹" + grouped + (remainder == 0 ? "" : "." + String.format("%02d", remainder));
    }
}
