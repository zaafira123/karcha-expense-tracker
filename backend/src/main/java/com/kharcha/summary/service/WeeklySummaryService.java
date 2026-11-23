package com.kharcha.summary.service;

import com.kharcha.budgets.domain.Budget;
import com.kharcha.budgets.repository.BudgetRepository;
import com.kharcha.common.domain.Category;
import com.kharcha.common.util.MonthUtils;
import com.kharcha.summary.domain.WeeklySummaryCalculator;
import com.kharcha.summary.dto.WeeklySummaryResponse;
import com.kharcha.summary.dto.MonthlySummaryResponse;
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
import java.util.Comparator;

@Service
public class WeeklySummaryService {
    private final TransactionRepository transactions;
    private final BudgetRepository budgets;
    private final Clock clock;

    public WeeklySummaryService(TransactionRepository transactions, BudgetRepository budgets, Clock clock) {
        this.transactions = transactions;
        this.budgets = budgets;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public WeeklySummaryResponse getWeeklySummary(UUID userId) {
        LocalDate end = LocalDate.now(clock);
        LocalDate start = end.minusDays(6);
        LocalDate previousStart = end.minusDays(13);
        LocalDate previousEnd = end.minusDays(7);
        YearMonth month = YearMonth.from(end);
        Map<Category, Long> current = spend(userId, start, end);
        Map<Category, Long> previous = spend(userId, previousStart, previousEnd);
        Map<Category, Long> monthSpend = spend(userId, month.atDay(1), end);
        Map<Category, Long> budgetAmounts = new EnumMap<>(Category.class);
        for (Budget budget : budgets.findByUserIdAndMonth(userId, month.toString())) {
            budgetAmounts.put(budget.getCategory(), budget.getAmountPaise());
        }
        return WeeklySummaryCalculator.calculate(start, end, current, previous, monthSpend, budgetAmounts);
    }

    @Transactional(readOnly = true)
    public MonthlySummaryResponse getMonthlySummary(UUID userId, String monthValue) {
        YearMonth month = MonthUtils.parseMonth(monthValue);
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        Map<Category, Long> current = spend(userId, start, end);
        Map<Category, Long> previous = spend(userId, month.minusMonths(1).atDay(1), month.minusMonths(1).atEndOfMonth());
        long currentTotal = current.values().stream().mapToLong(Long::longValue).sum();
        long previousTotal = previous.values().stream().mapToLong(Long::longValue).sum();
        long budgetTotal = budgets.findByUserIdAndMonth(userId, month.toString()).stream()
                .mapToLong(Budget::getAmountPaise).sum();
        List<MonthlySummaryResponse.CategoryTotal> top = current.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<Category, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(3)
                .map(entry -> new MonthlySummaryResponse.CategoryTotal(entry.getKey().name(), entry.getKey().getDisplayName(), entry.getValue()))
                .toList();
        List<String> tips = new java.util.ArrayList<>(3);
        tips.add(currentTotal == 0 ? "Add expenses as they happen to keep your month view useful."
                : "Review your recent expenses and correct any category that looks misplaced.");
        tips.add(budgetTotal == 0 ? "Set category budgets to compare spending with your monthly limits."
                : currentTotal * 100L >= budgetTotal * 80L ? "Review categories that are getting close to their monthly limits."
                : "Check your category budgets before planning larger purchases.");
        tips.add(top.isEmpty() ? "Choose categories for your expenses to see where your money goes."
                : "Review your spending in " + top.get(0).categoryName() + ", your highest recorded category this month.");
        String text = currentTotal == 0 ? "No expenses are recorded for " + month + "."
                : "You recorded " + money(currentTotal) + " in " + month + ", "
                + (currentTotal >= previousTotal ? "up " : "down ") + money(Math.abs(currentTotal - previousTotal))
                + " from the previous month.";
        return new MonthlySummaryResponse(month.toString(), currentTotal, previousTotal, currentTotal - previousTotal, top, tips, text);
    }

    private Map<Category, Long> spend(UUID userId, LocalDate start, LocalDate end) {
        Map<Category, Long> result = new EnumMap<>(Category.class);
        List<CategorySpendProjection> rows = transactions.findCategorySpendByUserIdAndOccurredOnBetween(userId, start, end);
        for (CategorySpendProjection row : rows) result.put(row.getCategory(), row.getTotalSpentPaise() == null ? 0L : row.getTotalSpentPaise());
        return result;
    }

    private static String money(long paise) {
        long rupees = paise / 100L;
        long remainder = Math.abs(paise % 100L);
        java.text.NumberFormat format = java.text.NumberFormat.getIntegerInstance(new java.util.Locale("en", "IN"));
        return "₹" + format.format(rupees) + (remainder == 0 ? "" : "." + String.format("%02d", remainder));
    }
}
