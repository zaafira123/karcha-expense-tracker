package com.kharcha.forecast.service;

import com.kharcha.budgets.domain.Budget;
import com.kharcha.budgets.repository.BudgetRepository;
import com.kharcha.common.domain.Category;
import com.kharcha.common.util.MonthUtils;
import com.kharcha.forecast.domain.ForecastCalculator;
import com.kharcha.forecast.domain.ForecastStatus;
import com.kharcha.forecast.dto.CategoryForecastResponse;
import com.kharcha.forecast.dto.ForecastResponse;
import com.kharcha.forecast.dto.ForecastSeriesPointResponse;
import com.kharcha.transactions.repository.CategoryDailySpendProjection;
import com.kharcha.transactions.repository.DailySpendProjection;
import com.kharcha.transactions.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ForecastService {

    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final Clock clock;

    public ForecastService(
            TransactionRepository transactionRepository,
            BudgetRepository budgetRepository,
            Clock clock
    ) {
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ForecastResponse getForecast(UUID userId, String monthStr) {
        YearMonth targetMonth = MonthUtils.parseMonth(monthStr);
        String formattedMonth = MonthUtils.formatMonth(targetMonth);

        LocalDate today = LocalDate.now(clock);
        YearMonth currentYearMonth = YearMonth.from(today);
        int daysInMonth = targetMonth.lengthOfMonth();

        int todayDay;
        if (targetMonth.equals(currentYearMonth)) {
            todayDay = today.getDayOfMonth();
        } else if (targetMonth.isBefore(currentYearMonth)) {
            // Past month: whole month has completed
            todayDay = daysInMonth;
        } else {
            // Future month: month has not started yet
            todayDay = 0;
        }

        // 1. Determine 30-day history window [today - 30 days, today - 1 day]
        LocalDate historyStart = today.minusDays(30);
        LocalDate historyEnd = today.minusDays(1);

        // 2. Fetch daily spend for the 30-day history window
        List<DailySpendProjection> historyDailySpendList =
                transactionRepository.findDailySpend(userId, historyStart, historyEnd);

        Map<LocalDate, Long> spendByDate = historyDailySpendList.stream()
                .collect(Collectors.toMap(
                        DailySpendProjection::getDate,
                        DailySpendProjection::getTotalPaise,
                        (existing, replacement) -> existing
                ));

        List<Long> history30 = new ArrayList<>(30);
        for (LocalDate d = historyStart; !d.isAfter(historyEnd); d = d.plusDays(1)) {
            history30.add(spendByDate.getOrDefault(d, 0L));
        }

        // 3. Fetch category daily spend for the 30-day history window
        List<CategoryDailySpendProjection> catHistoryList =
                transactionRepository.findCategoryDailySpend(userId, historyStart, historyEnd);

        Map<Category, Map<LocalDate, Long>> catSpendByDate = new HashMap<>();
        for (CategoryDailySpendProjection proj : catHistoryList) {
            catSpendByDate.computeIfAbsent(proj.getCategory(), k -> new HashMap<>())
                    .put(proj.getDate(), proj.getTotalPaise());
        }

        // 4. Fetch target month spend
        LocalDate monthStart = targetMonth.atDay(1);
        LocalDate effectiveMonthEnd;
        if (targetMonth.equals(currentYearMonth)) {
            effectiveMonthEnd = today;
        } else {
            effectiveMonthEnd = targetMonth.atEndOfMonth();
        }

        Map<Integer, Long> currentMonthDailySpend = new HashMap<>();
        long spentSoFarPaise = 0L;

        if (!targetMonth.isAfter(currentYearMonth)) {
            List<DailySpendProjection> monthSpendList =
                    transactionRepository.findDailySpend(userId, monthStart, effectiveMonthEnd);

            for (DailySpendProjection proj : monthSpendList) {
                currentMonthDailySpend.put(proj.getDate().getDayOfMonth(), proj.getTotalPaise());
                spentSoFarPaise += proj.getTotalPaise();
            }
        }

        // Target month category spend so far
        Map<Category, Long> catSpentSoFar = new HashMap<>();
        if (!targetMonth.isAfter(currentYearMonth)) {
            List<CategoryDailySpendProjection> monthCatSpendList =
                    transactionRepository.findCategoryDailySpend(userId, monthStart, effectiveMonthEnd);

            for (CategoryDailySpendProjection proj : monthCatSpendList) {
                catSpentSoFar.merge(proj.getCategory(), proj.getTotalPaise(), Long::sum);
            }
        }

        // 5. Fetch budgets for the month
        List<Budget> budgets = budgetRepository.findByUserIdAndMonth(userId, formattedMonth);
        Map<Category, Long> budgetMap = budgets.stream()
                .collect(Collectors.toMap(Budget::getCategory, Budget::getAmountPaise));

        long totalBudgetPaise = 0L;
        for (Category cat : Category.values()) {
            totalBudgetPaise += budgetMap.getOrDefault(cat, 0L);
        }

        // 6. Calculate total forecast
        ForecastCalculator.ForecastCalculationResult calcResult = ForecastCalculator.calculate(
                history30,
                spentSoFarPaise,
                todayDay,
                daysInMonth,
                totalBudgetPaise,
                currentMonthDailySpend
        );

        boolean hasSufficientTotalData = calcResult.status() != ForecastStatus.INSUFFICIENT_DATA;

        // 7. Calculate per-category forecast for all 8 categories
        List<CategoryForecastResponse> categoryResponses = new ArrayList<>(Category.values().length);
        int daysLeft = calcResult.daysLeft();

        for (Category cat : Category.values()) {
            Map<LocalDate, Long> catDates = catSpendByDate.getOrDefault(cat, Map.of());
            List<Long> catHistory30 = new ArrayList<>(30);
            for (LocalDate d = historyStart; !d.isAfter(historyEnd); d = d.plusDays(1)) {
                catHistory30.add(catDates.getOrDefault(d, 0L));
            }

            long catSpent = catSpentSoFar.getOrDefault(cat, 0L);
            long catBudget = budgetMap.getOrDefault(cat, 0L);

            ForecastCalculator.CategoryCalculationResult catResult = ForecastCalculator.calculateCategory(
                    cat,
                    catHistory30,
                    catSpent,
                    daysLeft,
                    catBudget,
                    hasSufficientTotalData
            );

            categoryResponses.add(new CategoryForecastResponse(
                    cat.name(),
                    cat.getDisplayName(),
                    catResult.projectedPaise(),
                    catResult.budgetPaise(),
                    catResult.overByPaise()
            ));
        }

        // 8. Map series
        List<ForecastSeriesPointResponse> seriesResponses = calcResult.series().stream()
                .map(pt -> new ForecastSeriesPointResponse(
                        pt.day(),
                        pt.actualPaise(),
                        pt.projectedPaise(),
                        pt.budgetPaise()
                ))
                .toList();

        return new ForecastResponse(
                formattedMonth,
                calcResult.status(),
                spentSoFarPaise,
                calcResult.projectedPaise(),
                calcResult.lowPaise(),
                calcResult.highPaise(),
                totalBudgetPaise,
                calcResult.avgDailyPaise(),
                calcResult.daysLeft(),
                seriesResponses,
                categoryResponses
        );
    }
}
