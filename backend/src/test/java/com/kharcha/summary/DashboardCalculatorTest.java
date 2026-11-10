package com.kharcha.summary;

import com.kharcha.common.domain.Category;
import com.kharcha.summary.domain.BudgetStatus;
import com.kharcha.summary.domain.DashboardCalculator;
import com.kharcha.summary.dto.CategoryProgressResponse;
import com.kharcha.summary.dto.DashboardResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardCalculatorTest {

    private final LocalDate today = LocalDate.of(2026, 10, 19);
    private final YearMonth currentMonth = YearMonth.of(2026, 10);

    @Test
    @DisplayName("Dashboard math: budget 6000 and spent 4200 gives remaining 1800, percentUsed 70, status OK")
    void testDashboardMathOkStatus() {
        Map<Category, Long> budgets = new EnumMap<>(Category.class);
        budgets.put(Category.FOOD_DINING, 600000L); // ₹6,000

        Map<Category, Long> spends = new EnumMap<>(Category.class);
        spends.put(Category.FOOD_DINING, 420000L); // ₹4,200

        DashboardResponse response = DashboardCalculator.calculate(
                currentMonth, today, budgets, spends, List.of());

        assertThat(response.totalBudgetPaise()).isEqualTo(600000L);
        assertThat(response.spentPaise()).isEqualTo(420000L);
        assertThat(response.remainingPaise()).isEqualTo(180000L);
        assertThat(response.daysLeft()).isEqualTo(12); // 31 - 19 = 12 days left

        CategoryProgressResponse food = response.categories().stream()
                .filter(c -> "FOOD_DINING".equals(c.category().code()))
                .findFirst().orElseThrow();

        assertThat(food.budgetPaise()).isEqualTo(600000L);
        assertThat(food.spentPaise()).isEqualTo(420000L);
        assertThat(food.percentUsed()).isEqualTo(70);
        assertThat(food.status()).isEqualTo(BudgetStatus.OK);
    }

    @Test
    @DisplayName("Dashboard math: status WARNING at 80 percent or more (spent 4800 on budget 5000)")
    void testDashboardMathWarningStatus() {
        Map<Category, Long> budgets = new EnumMap<>(Category.class);
        budgets.put(Category.SHOPPING, 500000L); // ₹5,000

        Map<Category, Long> spends = new EnumMap<>(Category.class);
        spends.put(Category.SHOPPING, 480000L); // ₹4,800 -> 96%

        DashboardResponse response = DashboardCalculator.calculate(
                currentMonth, today, budgets, spends, List.of());

        CategoryProgressResponse shopping = response.categories().stream()
                .filter(c -> "SHOPPING".equals(c.category().code()))
                .findFirst().orElseThrow();

        assertThat(shopping.percentUsed()).isEqualTo(96);
        assertThat(shopping.status()).isEqualTo(BudgetStatus.WARNING);
    }

    @Test
    @DisplayName("Dashboard math: spent 6600 on budget 6000 gives status OVER")
    void testDashboardMathOverStatus() {
        Map<Category, Long> budgets = new EnumMap<>(Category.class);
        budgets.put(Category.FOOD_DINING, 600000L);

        Map<Category, Long> spends = new EnumMap<>(Category.class);
        spends.put(Category.FOOD_DINING, 660000L);

        DashboardResponse response = DashboardCalculator.calculate(
                currentMonth, today, budgets, spends, List.of());

        CategoryProgressResponse food = response.categories().stream()
                .filter(c -> "FOOD_DINING".equals(c.category().code()))
                .findFirst().orElseThrow();

        assertThat(food.percentUsed()).isEqualTo(110);
        assertThat(food.status()).isEqualTo(BudgetStatus.OVER);
        assertThat(response.remainingPaise()).isEqualTo(-60000L);
    }

    @Test
    @DisplayName("Dashboard with no data returns zeros and all eight categories")
    void testDashboardWithNoData() {
        DashboardResponse response = DashboardCalculator.calculate(
                currentMonth, today, Map.of(), Map.of(), List.of());

        assertThat(response.totalBudgetPaise()).isEqualTo(0L);
        assertThat(response.spentPaise()).isEqualTo(0L);
        assertThat(response.remainingPaise()).isEqualTo(0L);
        assertThat(response.daysLeft()).isEqualTo(12);
        assertThat(response.categories()).hasSize(8);

        for (CategoryProgressResponse cat : response.categories()) {
            assertThat(cat.budgetPaise()).isEqualTo(0L);
            assertThat(cat.spentPaise()).isEqualTo(0L);
            assertThat(cat.percentUsed()).isEqualTo(0);
            assertThat(cat.status()).isEqualTo(BudgetStatus.OK);
        }
    }

    @Test
    @DisplayName("Days left calculation for past, current, and future months")
    void testDaysLeftCalculation() {
        // Current month (October 2026): 31 - 19 = 12
        assertThat(DashboardCalculator.calculateDaysLeft(YearMonth.of(2026, 10), today)).isEqualTo(12);

        // Past month (September 2026): 0 days left
        assertThat(DashboardCalculator.calculateDaysLeft(YearMonth.of(2026, 9), today)).isEqualTo(0);

        // Future month (November 2026): full month length = 30 days
        assertThat(DashboardCalculator.calculateDaysLeft(YearMonth.of(2026, 11), today)).isEqualTo(30);

        // Future leap February 2028: 29 days
        assertThat(DashboardCalculator.calculateDaysLeft(YearMonth.of(2028, 2), today)).isEqualTo(29);
    }
}
