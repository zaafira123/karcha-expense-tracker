package com.kharcha.transactions.service;

import com.kharcha.common.domain.Category;
import com.kharcha.common.error.ResourceNotFoundException;
import com.kharcha.common.util.MonthUtils;
import com.kharcha.transactions.domain.Transaction;
import com.kharcha.transactions.domain.TransactionSource;
import com.kharcha.transactions.dto.CreateTransactionRequest;
import com.kharcha.transactions.dto.TransactionResponse;
import com.kharcha.transactions.dto.UpdateTransactionRequest;
import com.kharcha.transactions.repository.TransactionRepository;
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
    private final Clock clock;

    public TransactionService(TransactionRepository transactionRepository, Clock clock) {
        this.transactionRepository = transactionRepository;
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
                TransactionSource.MANUAL,
                null,
                Instant.now(clock)
        );

        Transaction saved = transactionRepository.save(transaction);
        return TransactionResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> listTransactions(UUID userId, String monthStr, Category category) {
        YearMonth yearMonth = MonthUtils.parseMonth(monthStr);
        LocalDate startDate = MonthUtils.getStartDate(yearMonth);
        LocalDate endDate = MonthUtils.getEndDate(yearMonth);

        List<Transaction> transactions;
        if (category != null) {
            transactions = transactionRepository.findByUserIdAndCategoryAndOccurredOnBetweenOrderByOccurredOnDescCreatedAtDesc(
                    userId, category, startDate, endDate);
        } else {
            transactions = transactionRepository.findByUserIdAndOccurredOnBetweenOrderByOccurredOnDescCreatedAtDesc(
                    userId, startDate, endDate);
        }

        return transactions.stream()
                .map(TransactionResponse::from)
                .toList();
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
        return TransactionResponse.from(saved);
    }

    @Transactional
    public void deleteTransaction(UUID userId, UUID id) {
        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        transactionRepository.delete(transaction);
    }
}
