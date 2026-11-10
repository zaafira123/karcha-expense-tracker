package com.kharcha.summary.domain;

import com.kharcha.common.domain.Category;
import com.kharcha.summary.dto.CategoryProgressResponse;
import com.kharcha.summary.dto.DashboardResponse;
import com.kharcha.transactions.domain.Transaction;
import com.kharcha.transactions.dto.TransactionResponse;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class DashboardCalculator {

    private DashboardCalculator() {}

    public static DashboardResponse calculate(
            YearMonth targetMonth,
            LocalDate today,
            Map<Category, Long> budgetMap,
            Map<Category, Long> spendMap,
            List<Transaction> recentTransactions
    ) {
        int daysLeft = calculateDaysLeft(targetMonth, today);

        long totalBudgetPaise = 0L;
        long totalSpentPaise = 0L;
        List<CategoryProgressResponse> categoryProgressList = new ArrayList<>();

        for (Category category : Category.values()) {
            long budget = budgetMap.getOrDefault(category, 0L);
            long spent = spendMap.getOrDefault(category, 0L);

            totalBudgetPaise += budget;
            totalSpentPaise += spent;

            int percentUsed;
            if (budget > 0) {
                percentUsed = (int) ((spent * 100L) / budget);
            } else {
                percentUsed = spent > 0 ? 100 : 0;
            }

            BudgetStatus status;
            if (budget > 0) {
                if (spent > budget) {
                    status = BudgetStatus.OVER;
                } else if (percentUsed >= 80) {
                    status = BudgetStatus.WARNING;
                } else {
                    status = BudgetStatus.OK;
                }
            } else {
                if (spent > 0) {
                    status = BudgetStatus.OVER;
                } else {
                    status = BudgetStatus.OK;
                }
            }

            categoryProgressList.add(new CategoryProgressResponse(
                    category.toInfo(),
                    budget,
                    spent,
                    percentUsed,
                    status
            ));
        }

        long remainingPaise = totalBudgetPaise - totalSpentPaise;

        List<TransactionResponse> recentResponses = recentTransactions != null
                ? recentTransactions.stream().map(TransactionResponse::from).toList()
                : List.of();

        return new DashboardResponse(
                totalBudgetPaise,
                totalSpentPaise,
                remainingPaise,
                daysLeft,
                categoryProgressList,
                recentResponses
        );
    }

    public static int calculateDaysLeft(YearMonth targetMonth, LocalDate today) {
        YearMonth currentMonth = YearMonth.from(today);

        if (targetMonth.isBefore(currentMonth)) {
            return 0;
        } else if (targetMonth.isAfter(currentMonth)) {
            return targetMonth.lengthOfMonth();
        } else {
            return targetMonth.lengthOfMonth() - today.getDayOfMonth();
        }
    }
}
