package com.kharcha.budgets.service;

import com.kharcha.budgets.domain.Budget;
import com.kharcha.budgets.dto.CategoryBudgetResponse;
import com.kharcha.budgets.dto.UpdateBudgetRequest;
import com.kharcha.budgets.repository.BudgetRepository;
import com.kharcha.common.domain.Category;
import com.kharcha.common.util.MonthUtils;
import com.kharcha.transactions.repository.CategorySpendProjection;
import com.kharcha.transactions.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;

    public BudgetService(BudgetRepository budgetRepository, TransactionRepository transactionRepository) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryBudgetResponse> getBudgets(UUID userId, String monthStr) {
        YearMonth yearMonth = MonthUtils.parseMonth(monthStr);
        LocalDate startDate = MonthUtils.getStartDate(yearMonth);
        LocalDate endDate = MonthUtils.getEndDate(yearMonth);

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

        List<CategoryBudgetResponse> responses = new ArrayList<>();
        for (Category category : Category.values()) {
            Long budgetPaise = budgetMap.getOrDefault(category, 0L);
            Long spentPaise = spendMap.getOrDefault(category, 0L);
            responses.add(new CategoryBudgetResponse(category.toInfo(), budgetPaise, spentPaise));
        }

        return responses;
    }

    @Transactional
    public CategoryBudgetResponse updateBudget(UUID userId, Category category, String monthStr, UpdateBudgetRequest request) {
        YearMonth yearMonth = MonthUtils.parseMonth(monthStr);
        LocalDate startDate = MonthUtils.getStartDate(yearMonth);
        LocalDate endDate = MonthUtils.getEndDate(yearMonth);

        Budget budget = budgetRepository.findByUserIdAndCategoryAndMonth(userId, category, monthStr)
                .orElseGet(() -> new Budget(UUID.randomUUID(), userId, category, monthStr, request.amountPaise()));

        budget.setAmountPaise(request.amountPaise());
        Budget saved = budgetRepository.save(budget);

        List<CategorySpendProjection> spends = transactionRepository.findCategorySpendByUserIdAndOccurredOnBetween(
                userId, startDate, endDate);
        Long spentPaise = 0L;
        for (CategorySpendProjection s : spends) {
            if (s.getCategory() == category) {
                spentPaise = s.getTotalSpentPaise() != null ? s.getTotalSpentPaise() : 0L;
                break;
            }
        }

        return new CategoryBudgetResponse(category.toInfo(), saved.getAmountPaise(), spentPaise);
    }
}
