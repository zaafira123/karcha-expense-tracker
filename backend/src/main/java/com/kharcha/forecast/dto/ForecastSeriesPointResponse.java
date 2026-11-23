package com.kharcha.forecast.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Single daily point for the cumulative forecast chart")
public record ForecastSeriesPointResponse(
        @Schema(description = "Day of the month (1..31)", example = "1")
        int day,

        @Schema(description = "Cumulative actual spend up to this day, null after today", example = "150000")
        Long actualPaise,

        @Schema(description = "Projected cumulative spend from today onwards, null before today", example = "2845000")
        Long projectedPaise,

        @Schema(description = "Flat total monthly budget line", example = "4200000")
        long budgetPaise
) {}
