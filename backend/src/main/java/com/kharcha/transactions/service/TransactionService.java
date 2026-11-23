package com.kharcha.transactions.service;

import com.kharcha.agent.event.CategoryThresholdEvent;
import com.kharcha.budgets.domain.Budget;
import com.kharcha.budgets.repository.BudgetRepository;
import com.kharcha.common.domain.Category;
import com.kharcha.common.error.ResourceNotFoundException;
import com.kharcha.common.util.MonthUtils;
import com.kharcha.parsing.service.MerchantCorrectionService;
import com.kharcha.transactions.domain.Transaction;
import com.kharcha.transactions.domain.TransactionSource;
import com.kharcha.transactions.dto.CreateTransactionRequest;
import com.kharcha.transactions.dto.TransactionResponse;
import com.kharcha.transactions.dto.UpdateTransactionRequest;
import com.kharcha.transactions.repository.CategorySpendProjection;
import com.kharcha.transactions.repository.TransactionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final MerchantCorrectionService merchantCorrectionService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public TransactionService(
            TransactionRepository transactionRepository,
            BudgetRepository budgetRepository,
            MerchantCorrectionService merchantCorrectionService,
            ApplicationEventPublisher eventPublisher,
            Clock clock
    ) {
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.merchantCorrectionService = merchantCorrectionService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public TransactionResponse createTransaction(UUID userId, CreateTransactionRequest request) {
        LocalDate today = LocalDate.now(clock);
        LocalDate occurredOn = request.occurredOn() != null ? request.occurredOn() : today;

        if (occurredOn.isAfter(today)) {
            throw new IllegalArgumentException("Transaction date cannot be in the future");
        }

        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                userId,
                request.amountPaise(),
                request.category(),
                request.merchant(),
                occurredOn,
                request.note(),
                request.source() != null ? request.source() : TransactionSource.MANUAL,
                null,
                Instant.now(clock)
        );

        Transaction saved = transactionRepository.save(transaction);

        if (request.merchant() != null && !request.merchant().isBlank() && request.category() != null) {
            merchantCorrectionService.recordCorrection(userId, request.merchant(), request.category());
        }

        // Fire event if the category's spend has crossed ≥ 80% of its monthly budget.
        if (request.category() != null) {
            publishThresholdEventIfNeeded(userId, request.category(), occurredOn);
        }

        return TransactionResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> listTransactions(UUID userId, String monthStr, Category category) {
        YearMonth yearMonth = MonthUtils.parseMonth(monthStr);
        LocalDate startDate = MonthUtils.getStartDate(yearMonth);
        LocalDate endDate = MonthUtils.getEndDate(yearMonth);

        List<Transaction> transactions;
        if (category != null) {
            transactions = transactionRepository
                    .findByUserIdAndCategoryAndOccurredOnBetweenOrderByOccurredOnDescCreatedAtDesc(
                            userId, category, startDate, endDate);
        } else {
            transactions = transactionRepository
                    .findByUserIdAndOccurredOnBetweenOrderByOccurredOnDescCreatedAtDesc(
                            userId, startDate, endDate);
        }
        return transactions.stream().map(TransactionResponse::from).toList();
    }

    @Transactional
    public TransactionResponse updateTransaction(UUID userId, UUID id, UpdateTransactionRequest request) {
        LocalDate today = LocalDate.now(clock);
        if (request.occurredOn().isAfter(today)) {
            throw new IllegalArgumentException("Transaction date cannot be in the future");
        }

        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        transaction.setAmountPaise(request.amountPaise());
        transaction.setCategory(request.category());
        transaction.setMerchant(request.merchant());
        transaction.setOccurredOn(request.occurredOn());
        transaction.setNote(request.note());

        Transaction saved = transactionRepository.save(transaction);

        if (request.merchant() != null && !request.merchant().isBlank() && request.category() != null) {
            merchantCorrectionService.recordCorrection(userId, request.merchant(), request.category());
        }

        return TransactionResponse.from(saved);
    }

    @Transactional
    public void deleteTransaction(UUID userId, UUID id) {
        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        transactionRepository.delete(transaction);
    }

    // -------------------------------------------------------------------------
    // Internal helper
    // -------------------------------------------------------------------------

    /**
     * Checks if the given category's month-to-date spend has reached ≥ 80% of its
     * budget. If so, publishes a {@link CategoryThresholdEvent} so the agent can
     * react without TransactionService having a direct dependency on AgentService.
     */
    private void publishThresholdEventIfNeeded(UUID userId, Category category, LocalDate occurredOn) {
        YearMonth ym = YearMonth.from(occurredOn);
        String monthStr = MonthUtils.formatMonth(ym);

        Budget budget = budgetRepository
                .findByUserIdAndCategoryAndMonth(userId, category, monthStr)
                .orElse(null);

        if (budget == null || budget.getAmountPaise() <= 0L) {
            return; // No budget set → cannot compute ratio.
        }

        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        List<CategorySpendProjection> spends = transactionRepository
                .findCategorySpendByUserIdAndOccurredOnBetween(userId, monthStart, monthEnd);

        long spentPaise = 0L;
        for (CategorySpendProjection s : spends) {
            if (s.getCategory() == category) {
                spentPaise = s.getTotalSpentPaise() != null ? s.getTotalSpentPaise() : 0L;
                break;
            }
        }

        // Publish if spend * 100 >= budget * 80  (i.e. ≥ 80%).
        if (spentPaise * 100L >= budget.getAmountPaise() * 80L) {
            eventPublisher.publishEvent(new CategoryThresholdEvent(userId, category, monthStr));
        }
    }
}
