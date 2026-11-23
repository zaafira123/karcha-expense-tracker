package com.kharcha.parsing.controller;

import com.kharcha.parsing.dto.ParseExpenseRequest;
import com.kharcha.parsing.dto.ParseExpenseResponse;
import com.kharcha.parsing.service.ExpenseParsingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/expenses")
@Tag(name = "Expense Parsing", description = "Parse natural language expense sentences into draft expenses")
public class ExpenseParsingController {

    private final ExpenseParsingService parsingService;

    public ExpenseParsingController(ExpenseParsingService parsingService) {
        this.parsingService = parsingService;
    }

    @PostMapping("/parse")
    @Operation(summary = "Parse expense sentence", description = "Extracts amount, category, merchant, and date into a draft expense without saving")
    public ResponseEntity<ParseExpenseResponse> parseExpense(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ParseExpenseRequest request
    ) {
        ParseExpenseResponse response = parsingService.parseExpense(userId, request);
        return ResponseEntity.ok(response);
    }
}
