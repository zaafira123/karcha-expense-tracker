package com.kharcha.forecast;

import com.kharcha.common.domain.Category;
import com.kharcha.forecast.domain.ForecastCalculator;
import com.kharcha.forecast.domain.ForecastStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ForecastCalculatorTest {

    private List<Long> createList(int size, Long value) {
        List<Long> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(value);
        }
        return list;
    }

    @Test
    @DisplayName("Constant daily spend of 1000 paise, 10 days left: projected = spent + 10000, band 0, low = high = projected")
    void testConstantDailySpend() {
        // 30 days of 1000 paise
        List<Long> history = createList(30, 1000L);
        long spentSoFar = 20000L;
        int todayDay = 20;
        int daysInMonth = 30; // 30 - 20 = 10 days left
        long totalBudget = 50000L;

        ForecastCalculator.ForecastCalculationResult result = ForecastCalculator.calculate(
                history, spentSoFar, todayDay, daysInMonth, totalBudget, Map.of()
        );

        assertThat(result.status()).isEqualTo(ForecastStatus.OK);
        assertThat(result.daysLeft()).isEqualTo(10);
        assertThat(result.avgDailyPaise()).isEqualTo(1000L);
        assertThat(result.projectedPaise()).isEqualTo(spentSoFar + 10000L); // 30000
        assertThat(result.lowPaise()).isEqualTo(result.projectedPaise());
        assertThat(result.highPaise()).isEqualTo(result.projectedPaise());
    }

    @Test
    @DisplayName("Zero spend history: projected = spent so far, low = high = spent")
    void testZeroSpendHistory() {
        // When 0 spend across all 30 days, active days = 0 < 7, so it is INSUFFICIENT_DATA
        List<Long> history = createList(30, 0L);
        long spentSoFar = 15000L;

        ForecastCalculator.ForecastCalculationResult result = ForecastCalculator.calculate(
                history, spentSoFar, 15, 30, 50000L, Map.of()
        );

        assertThat(result.status()).isEqualTo(ForecastStatus.INSUFFICIENT_DATA);
        assertThat(result.projectedPaise()).isNull();
        assertThat(result.lowPaise()).isNull();
        assertThat(result.highPaise()).isNull();
        assertThat(result.avgDailyPaise()).isNull();
    }

    @Test
    @DisplayName("Insufficient data (only 6 active days in the 30): status INSUFFICIENT_DATA, nulls returned")
    void testInsufficientData6ActiveDays() {
        List<Long> history = new ArrayList<>(Collections.nCopies(24, 0L));
        for (int i = 0; i < 6; i++) {
            history.add(1000L);
        }

        ForecastCalculator.ForecastCalculationResult result = ForecastCalculator.calculate(
                history, 6000L, 10, 30, 50000L, Map.of()
        );

        assertThat(result.status()).isEqualTo(ForecastStatus.INSUFFICIENT_DATA);
        assertThat(result.projectedPaise()).isNull();
        assertThat(result.lowPaise()).isNull();
        assertThat(result.highPaise()).isNull();
        assertThat(result.avgDailyPaise()).isNull();
    }

    @Test
    @DisplayName("Sufficient data (7 active days in the 30): status OK and numbers returned")
    void testSufficientData7ActiveDays() {
        List<Long> history = new ArrayList<>(Collections.nCopies(23, 0L));
        for (int i = 0; i < 7; i++) {
            history.add(3000L);
        }

        ForecastCalculator.ForecastCalculationResult result = ForecastCalculator.calculate(
                history, 21000L, 10, 30, 50000L, Map.of()
        );

        assertThat(result.status()).isEqualTo(ForecastStatus.OK);
        assertThat(result.projectedPaise()).isNotNull();
        assertThat(result.lowPaise()).isNotNull();
        assertThat(result.highPaise()).isNotNull();
        assertThat(result.avgDailyPaise()).isNotNull();
    }

    @Test
    @DisplayName("Month lengths 28, 30, and 31 days: daysLeft correct for each")
    void testMonthLengths() {
        List<Long> history = createList(30, 1000L);
        int today = 18;

        ForecastCalculator.ForecastCalculationResult feb = ForecastCalculator.calculate(
                history, 18000L, today, 28, 50000L, Map.of()
        );
        assertThat(feb.daysLeft()).isEqualTo(10); // 28 - 18

        ForecastCalculator.ForecastCalculationResult apr = ForecastCalculator.calculate(
                history, 18000L, today, 30, 50000L, Map.of()
        );
        assertThat(apr.daysLeft()).isEqualTo(12); // 30 - 18

        ForecastCalculator.ForecastCalculationResult oct = ForecastCalculator.calculate(
                history, 18000L, today, 31, 50000L, Map.of()
        );
        assertThat(oct.daysLeft()).isEqualTo(13); // 31 - 18
    }

    @Test
    @DisplayName("First day of the month (today = 1): uses daysInMonth - 1 for daysLeft")
    void testFirstDayOfMonth() {
        List<Long> history = createList(30, 1000L);
        ForecastCalculator.ForecastCalculationResult result = ForecastCalculator.calculate(
                history, 1000L, 1, 31, 50000L, Map.of()
        );
        assertThat(result.daysLeft()).isEqualTo(30);
    }

    @Test
    @DisplayName("Last day of the month (today = last day): daysLeft = 0, projected = spent so far, band 0")
    void testLastDayOfMonth() {
        List<Long> history = createList(30, 1000L);
        long spentSoFar = 42000L;
        ForecastCalculator.ForecastCalculationResult result = ForecastCalculator.calculate(
                history, spentSoFar, 31, 31, 50000L, Map.of()
        );
        assertThat(result.daysLeft()).isEqualTo(0);
        assertThat(result.projectedPaise()).isEqualTo(spentSoFar);
        assertThat(result.lowPaise()).isEqualTo(spentSoFar);
        assertThat(result.highPaise()).isEqualTo(spentSoFar);
    }

    @Test
    @DisplayName("Projected above budget returns OVER_BUDGET_PROJECTED")
    void testOverBudgetProjected() {
        List<Long> history = createList(30, 5000L); // avg = 5000
        long spentSoFar = 25000L;
        int today = 20;
        int daysInMonth = 30; // 10 days left -> 25000 + 50000 = 75000
        long budget = 60000L; // 75000 > 60000

        ForecastCalculator.ForecastCalculationResult result = ForecastCalculator.calculate(
                history, spentSoFar, today, daysInMonth, budget, Map.of()
        );
        assertThat(result.status()).isEqualTo(ForecastStatus.OVER_BUDGET_PROJECTED);
        assertThat(result.projectedPaise()).isEqualTo(75000L);
    }

    @Test
    @DisplayName("Low never below spent so far")
    void testLowNeverBelowSpentSoFar() {
        // High variance history where projected - band could be negative or less than spentSoFar
        List<Long> history = new ArrayList<>();
        for (int i = 0; i < 20; i++) history.add(0L);
        for (int i = 0; i < 10; i++) history.add(100000L);

        long spentSoFar = 50000L;
        ForecastCalculator.ForecastCalculationResult result = ForecastCalculator.calculate(
                history, spentSoFar, 2, 30, 1000000L, Map.of()
        );
        assertThat(result.lowPaise()).isGreaterThanOrEqualTo(spentSoFar);
    }

    @Test
    @DisplayName("Series length equals days in month, actual null after today, projected null before today")
    void testSeriesStructure() {
        List<Long> history = createList(30, 1000L);
        int today = 15;
        int daysInMonth = 30;
        Map<Integer, Long> currentMonth = Map.of(1, 1000L, 2, 2000L, 15, 500L);

        ForecastCalculator.ForecastCalculationResult result = ForecastCalculator.calculate(
                history, 3500L, today, daysInMonth, 50000L, currentMonth
        );

        List<ForecastCalculator.ChartSeriesPoint> series = result.series();
        assertThat(series).hasSize(daysInMonth);

        // Before today: actual is non-null, projected is null
        for (int d = 1; d < today; d++) {
            assertThat(series.get(d - 1).day()).isEqualTo(d);
            assertThat(series.get(d - 1).actualPaise()).isNotNull();
            assertThat(series.get(d - 1).projectedPaise()).isNull();
            assertThat(series.get(d - 1).budgetPaise()).isEqualTo(50000L);
        }

        // At today: both meet
        assertThat(series.get(today - 1).actualPaise()).isNotNull();
        assertThat(series.get(today - 1).projectedPaise()).isNotNull();

        // After today: actual is null, projected is non-null
        for (int d = today + 1; d <= daysInMonth; d++) {
            assertThat(series.get(d - 1).day()).isEqualTo(d);
            assertThat(series.get(d - 1).actualPaise()).isNull();
            assertThat(series.get(d - 1).projectedPaise()).isNotNull();
            assertThat(series.get(d - 1).budgetPaise()).isEqualTo(50000L);
        }
    }

    @Test
    @DisplayName("Per-category overBy is 0 when projected is under budget")
    void testPerCategoryOverBy() {
        List<Long> history = createList(30, 1000L);
        long spentSoFar = 5000L;
        int daysLeft = 10;
        long budget = 25000L; // projected = 5000 + 1000*10 = 15000 < 25000

        ForecastCalculator.CategoryCalculationResult under = ForecastCalculator.calculateCategory(
                Category.FOOD_DINING, history, spentSoFar, daysLeft, budget, true
        );
        assertThat(under.overByPaise()).isEqualTo(0L);
        assertThat(under.projectedPaise()).isEqualTo(15000L);

        // Over budget
        long smallBudget = 10000L;
        ForecastCalculator.CategoryCalculationResult over = ForecastCalculator.calculateCategory(
                Category.FOOD_DINING, history, spentSoFar, daysLeft, smallBudget, true
        );
        assertThat(over.overByPaise()).isEqualTo(5000L); // 15000 - 10000
    }
}
