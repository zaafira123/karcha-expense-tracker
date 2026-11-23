package com.kharcha.forecast.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Category month-end projection and budget shortfall")
public record CategoryForecastResponse(
        @Schema(description = "Category code", example = "FOOD_DINING")
        String category,

        @Schema(description = "Category user-facing display name", example = "Food and dining")
        String categoryName,

        @Schema(description = "Projected month-end spend in paise, null if insufficient data", example = "780000")
        Long projectedPaise,

        @Schema(description = "Monthly budget allocated for this category", example = "600000")
        long budgetPaise,

        @Schema(description = "Amount projected to exceed budget, or 0 if within budget", example = "180000")
        long overByPaise
) {}
