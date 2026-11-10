package com.kharcha.budgets.controller;

import com.kharcha.budgets.dto.CategoryBudgetResponse;
import com.kharcha.budgets.dto.UpdateBudgetRequest;
import com.kharcha.budgets.service.BudgetService;
import com.kharcha.common.domain.Category;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/budgets")
@Tag(name = "Budgets", description = "Monthly budget management endpoints")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    @Operation(summary = "Get monthly budgets", description = "Returns all 8 category budgets and spent amounts for a month")
    public ResponseEntity<List<CategoryBudgetResponse>> getBudgets(
            @AuthenticationPrincipal UUID userId,
            @RequestParam("month") String month
    ) {
        List<CategoryBudgetResponse> response = budgetService.getBudgets(userId, month);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{category}")
    @Operation(summary = "Set or update category budget", description = "Sets or updates the monthly budget limit for a category")
    public ResponseEntity<CategoryBudgetResponse> updateBudget(
            @AuthenticationPrincipal UUID userId,
            @PathVariable("category") Category category,
            @RequestParam("month") String month,
            @Valid @RequestBody UpdateBudgetRequest request
    ) {
        CategoryBudgetResponse response = budgetService.updateBudget(userId, category, month, request);
        return ResponseEntity.ok(response);
    }
}
