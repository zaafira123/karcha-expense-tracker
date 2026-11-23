package com.kharcha.summary;

import com.kharcha.common.domain.Category;
import com.kharcha.summary.domain.WeeklySummaryCalculator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WeeklySummaryCalculatorTest {
    @Test
    void calculatesChangeTopThreeIncreasesAndOverBudgetAlerts() {
        var result = WeeklySummaryCalculator.calculate(LocalDate.parse("2026-11-09"), LocalDate.parse("2026-11-15"),
                Map.of(Category.GROCERIES, 50000L, Category.TRAVEL, 30000L, Category.FOOD_DINING, 25000L),
                Map.of(Category.GROCERIES, 20000L, Category.TRAVEL, 40000L),
                Map.of(Category.GROCERIES, 90000L), Map.of(Category.GROCERIES, 100000L));

        assertThat(result.currentWeekSpentPaise()).isEqualTo(105000L);
        assertThat(result.previousWeekSpentPaise()).isEqualTo(60000L);
        assertThat(result.changePaise()).isEqualTo(45000L);
        assertThat(result.categoryIncreases()).extracting("category").contains("GROCERIES", "FOOD_DINING");
        assertThat(result.topCategories()).hasSize(3);
        assertThat(result.budgetsOver80()).singleElement().extracting("percentUsed").isEqualTo(90);
        assertThat(result.summaryText()).contains("last 7 days");
    }
}
