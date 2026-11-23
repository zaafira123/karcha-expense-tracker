package com.kharcha.forecast.controller;

import com.kharcha.forecast.dto.ForecastResponse;
import com.kharcha.forecast.service.ForecastService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/forecast")
@Tag(name = "Forecast", description = "Month-end cumulative spending forecast and chart series")
public class ForecastController {

    private final ForecastService forecastService;

    public ForecastController(ForecastService forecastService) {
        this.forecastService = forecastService;
    }

    @GetMapping
    @Operation(summary = "Get monthly spend forecast", description = "Returns projected month-end spend, range, and chart series based on 30-day spending history")
    public ResponseEntity<ForecastResponse> getForecast(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Month in YYYY-MM format", example = "2026-10")
            @RequestParam("month") String month
    ) {
        ForecastResponse response = forecastService.getForecast(userId, month);
        return ResponseEntity.ok(response);
    }
}
