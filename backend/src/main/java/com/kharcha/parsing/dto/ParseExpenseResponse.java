package com.kharcha.parsing.dto;

import com.kharcha.parsing.engine.ParseConfidence;
import com.kharcha.parsing.engine.ParseMethod;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Parsed draft expense representation")
public record ParseExpenseResponse(
        @Schema(description = "Extracted amount in paise (integer)", example = "45000")
        Long amountPaise,

        @Schema(description = "Category code", example = "FOOD_DINING")
        String category,

        @Schema(description = "User-friendly category display name", example = "Food and dining")
        String categoryName,

        @Schema(description = "Extracted merchant or description", example = "Pizza with friends")
        String merchant,

        @Schema(description = "Expense date (YYYY-MM-DD)", example = "2026-11-10")
        LocalDate occurredOn,

        @Schema(description = "Original or trimmed input text", example = "paid 450 for pizza with friends")
        String note,

        @Schema(description = "Expense source indicator", example = "TEXT")
        String source,

        @Schema(description = "Parse confidence level", example = "HIGH")
        ParseConfidence confidence,

        @Schema(description = "Whether user confirmation or manual review is needed", example = "false")
        boolean needsReview,

        @Schema(description = "Categorization method used", example = "RULES")
        ParseMethod method
) {}
