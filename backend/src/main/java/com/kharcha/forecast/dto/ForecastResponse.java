package com.kharcha.forecast.dto;

import com.kharcha.forecast.domain.ForecastStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Monthly spend forecast and chart series")
public record ForecastResponse(
        @Schema(description = "Month in YYYY-MM format", example = "2026-10")
        String month,

        @Schema(description = "Forecast status", example = "OK")
        ForecastStatus status,

        @Schema(description = "Spent so far in the month in paise", example = "2845000")
        long spentPaise,

        @Schema(description = "Projected month-end spend in paise, null if insufficient data", example = "3840000")
        Long projectedPaise,

        @Schema(description = "Lower bound of projection range, null if insufficient data", example = "3590000")
        Long lowPaise,

        @Schema(description = "Upper bound of projection range, null if insufficient data", example = "4120000")
        Long highPaise,

        @Schema(description = "Total monthly budget in paise", example = "4200000")
        long budgetPaise,

        @Schema(description = "30-day average daily spend in paise, null if insufficient data", example = "82900")
        Long avgDailyPaise,

        @Schema(description = "Days remaining after today in the month", example = "12")
        int daysLeft,

        @Schema(description = "Daily chart series points for actual, projected, and budget lines")
        List<ForecastSeriesPointResponse> series,

        @Schema(description = "Per-category projection and budget analysis")
        List<CategoryForecastResponse> categories
) {}
