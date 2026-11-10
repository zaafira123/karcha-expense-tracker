package com.kharcha.summary.dto;

import com.kharcha.transactions.dto.TransactionResponse;

import java.util.List;

public record DashboardResponse(
        Long totalBudgetPaise,
        Long spentPaise,
        Long remainingPaise,
        int daysLeft,
        List<CategoryProgressResponse> categories,
        List<TransactionResponse> recent
) {}
