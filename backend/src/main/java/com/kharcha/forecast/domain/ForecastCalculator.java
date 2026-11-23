package com.kharcha.forecast.domain;

import com.kharcha.common.domain.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.math.BigInteger;

public final class ForecastCalculator {

    private ForecastCalculator() {}

    public record ChartSeriesPoint(
            int day,
            Long actualPaise,
            Long projectedPaise,
            long budgetPaise
    ) {}

    public record ForecastCalculationResult(
            ForecastStatus status,
            Long projectedPaise,
            Long lowPaise,
            Long highPaise,
            Long avgDailyPaise,
            int daysLeft,
            List<ChartSeriesPoint> series
    ) {}

    public record CategoryCalculationResult(
            Category category,
            Long projectedPaise,
            long budgetPaise,
            long overByPaise
    ) {}

    public static ForecastCalculationResult calculate(
            List<Long> dailySpend30Days,
            long spentSoFarPaise,
            int todayDay,
            int daysInMonth,
            long totalBudgetPaise,
            Map<Integer, Long> currentMonthDailySpend
    ) {
        int daysLeft = Math.max(0, daysInMonth - todayDay);

        // 1. Check active days in the 30-day window
        int activeDays = 0;
        long sum30 = 0L;
        if (dailySpend30Days != null) {
            for (Long spend : dailySpend30Days) {
                if (spend != null && spend > 0L) {
                    activeDays++;
                }
                if (spend != null) {
                    sum30 += spend;
                }
            }
        }

        boolean hasSufficientData = activeDays >= 7;

        ForecastStatus status;
        Long projectedPaise = null;
        Long lowPaise = null;
        Long highPaise = null;
        Long avgDailyPaise = null;

        if (!hasSufficientData) {
            status = ForecastStatus.INSUFFICIENT_DATA;
        } else {
            // Mean of 30 values rounded to nearest paise, using integer arithmetic.
            long avgDaily = (sum30 + 15L) / 30L;
            avgDailyPaise = avgDaily;

            // Population variance numerator. BigInteger avoids overflow when squaring paise.
            BigInteger varianceNumerator = BigInteger.ZERO;
            if (dailySpend30Days != null) {
                for (Long spend : dailySpend30Days) {
                    long val = spend != null ? spend : 0L;
                    BigInteger diff = BigInteger.valueOf(val).multiply(BigInteger.valueOf(30L))
                            .subtract(BigInteger.valueOf(sum30));
                    varianceNumerator = varianceNumerator.add(diff.multiply(diff));
                }
            }

            // Projected spend
            long projected = spentSoFarPaise + (avgDaily * daysLeft);
            projectedPaise = projected;

            // Band and range
            // sqrt(population variance * days left), rounded down to whole paise.
            BigInteger bandSquared = varianceNumerator.multiply(BigInteger.valueOf(daysLeft))
                    .divide(BigInteger.valueOf(27000L));
            long bandPaise = bandSquared.sqrt().min(BigInteger.valueOf(Long.MAX_VALUE)).longValue();

            lowPaise = Math.max(spentSoFarPaise, projected - bandPaise);
            highPaise = projected + bandPaise;

            if (projected > totalBudgetPaise) {
                status = ForecastStatus.OVER_BUDGET_PROJECTED;
            } else {
                status = ForecastStatus.OK;
            }
        }

        // 2. Build cumulative daily series for the chart
        List<ChartSeriesPoint> series = new ArrayList<>(daysInMonth);
        long cumulativeActual = 0L;

        for (int day = 1; day <= daysInMonth; day++) {
            Long actualPoint = null;
            if (day <= todayDay) {
                long daySpend = (currentMonthDailySpend != null)
                        ? currentMonthDailySpend.getOrDefault(day, 0L)
                        : 0L;
                cumulativeActual += daySpend;
                actualPoint = cumulativeActual;
            }

            Long projectedPoint = null;
            if (hasSufficientData && avgDailyPaise != null) {
                if (day >= todayDay) {
                    projectedPoint = spentSoFarPaise + (avgDailyPaise * (day - todayDay));
                }
            }

            series.add(new ChartSeriesPoint(day, actualPoint, projectedPoint, totalBudgetPaise));
        }

        return new ForecastCalculationResult(
                status,
                projectedPaise,
                lowPaise,
                highPaise,
                avgDailyPaise,
                daysLeft,
                series
        );
    }

    public static CategoryCalculationResult calculateCategory(
            Category category,
            List<Long> categoryDailySpend30Days,
            long categorySpentSoFarPaise,
            int daysLeft,
            long categoryBudgetPaise,
            boolean hasSufficientTotalData
    ) {
        if (!hasSufficientTotalData) {
            return new CategoryCalculationResult(category, null, categoryBudgetPaise, 0L);
        }

        long sum = 0L;
        if (categoryDailySpend30Days != null) {
            for (Long spend : categoryDailySpend30Days) {
                if (spend != null) {
                    sum += spend;
                }
            }
        }

        long avgDaily = (sum + 15L) / 30L;
        long projected = categorySpentSoFarPaise + (avgDaily * daysLeft);
        long overBy = Math.max(0L, projected - categoryBudgetPaise);

        return new CategoryCalculationResult(category, projected, categoryBudgetPaise, overBy);
    }
}
