package com.kharcha.summary.service;

import com.kharcha.budgets.domain.Budget;
import com.kharcha.budgets.repository.BudgetRepository;
import com.kharcha.common.domain.Category;
import com.kharcha.common.util.MonthUtils;
import com.kharcha.summary.domain.DashboardCalculator;
import com.kharcha.summary.dto.DashboardResponse;
import com.kharcha.transactions.domain.Transaction;
import com.kharcha.transactions.repository.CategorySpendProjection;
import com.kharcha.transactions.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DashboardService {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final Clock clock;

    public DashboardService(BudgetRepository budgetRepository,
                            TransactionRepository transactionRepository,
                            Clock clock) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(UUID userId, String monthStr) {
        YearMonth yearMonth = MonthUtils.parseMonth(monthStr);
        LocalDate startDate = MonthUtils.getStartDate(yearMonth);
        LocalDate endDate = MonthUtils.getEndDate(yearMonth);
        LocalDate today = LocalDate.now(clock);

        List<Budget> budgets = budgetRepository.findByUserIdAndMonth(userId, monthStr);
        Map<Category, Long> budgetMap = new EnumMap<>(Category.class);
        for (Budget b : budgets) {
            budgetMap.put(b.getCategory(), b.getAmountPaise());
        }

        List<CategorySpendProjection> spends = transactionRepository.findCategorySpendByUserIdAndOccurredOnBetween(
                userId, startDate, endDate);
        Map<Category, Long> spendMap = new EnumMap<>(Category.class);
        for (CategorySpendProjection s : spends) {
            spendMap.put(s.getCategory(), s.getTotalSpentPaise() != null ? s.getTotalSpentPaise() : 0L);
        }

        List<Transaction> recentTransactions = transactionRepository
                .findTop5ByUserIdAndOccurredOnBetweenOrderByOccurredOnDescCreatedAtDesc(userId, startDate, endDate);

        return DashboardCalculator.calculate(yearMonth, today, budgetMap, spendMap, recentTransactions);
    }
}
