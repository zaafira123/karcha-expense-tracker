package com.kharcha.summary.controller;

import com.kharcha.summary.dto.WeeklySummaryResponse;
import com.kharcha.summary.dto.MonthlySummaryResponse;
import com.kharcha.summary.service.WeeklySummaryService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@RestController
@RequestMapping("/api/summary")
public class WeeklySummaryController {
    private final WeeklySummaryService summaryService;

    public WeeklySummaryController(WeeklySummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @GetMapping("/weekly")
    @Operation(summary = "Get the last 7 days summary")
    public ResponseEntity<WeeklySummaryResponse> getWeeklySummary(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(summaryService.getWeeklySummary(userId));
    }

    @GetMapping("/monthly")
    @Operation(summary = "Get a monthly spending summary and tips")
    public ResponseEntity<MonthlySummaryResponse> getMonthlySummary(
            @AuthenticationPrincipal UUID userId,
            @RequestParam String month
    ) {
        return ResponseEntity.ok(summaryService.getMonthlySummary(userId, month));
    }
}
