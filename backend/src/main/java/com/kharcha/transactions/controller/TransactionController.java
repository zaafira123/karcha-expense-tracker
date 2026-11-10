package com.kharcha.transactions.controller;

import com.kharcha.common.domain.Category;
import com.kharcha.transactions.dto.CreateTransactionRequest;
import com.kharcha.transactions.dto.TransactionResponse;
import com.kharcha.transactions.dto.UpdateTransactionRequest;
import com.kharcha.transactions.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transactions", description = "Transaction management endpoints")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    @Operation(summary = "List transactions by month", description = "Lists transactions for a given month with optional category filter")
    public ResponseEntity<List<TransactionResponse>> listTransactions(
            @AuthenticationPrincipal UUID userId,
            @RequestParam("month") String month,
            @RequestParam(value = "category", required = false) Category category
    ) {
        List<TransactionResponse> responses = transactionService.listTransactions(userId, month, category);
        return ResponseEntity.ok(responses);
    }

    @PostMapping
    @Operation(summary = "Create manual transaction", description = "Creates a new manual expense transaction")
    public ResponseEntity<TransactionResponse> createTransaction(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateTransactionRequest request
    ) {
        TransactionResponse response = transactionService.createTransaction(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update transaction", description = "Updates an existing transaction")
    public ResponseEntity<TransactionResponse> updateTransaction(
            @AuthenticationPrincipal UUID userId,
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateTransactionRequest request
    ) {
        TransactionResponse response = transactionService.updateTransaction(userId, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete transaction", description = "Deletes an existing transaction")
    public ResponseEntity<Void> deleteTransaction(
            @AuthenticationPrincipal UUID userId,
            @PathVariable("id") UUID id
    ) {
        transactionService.deleteTransaction(userId, id);
        return ResponseEntity.noContent().build();
    }
}
