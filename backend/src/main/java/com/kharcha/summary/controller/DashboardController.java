package com.kharcha.summary.controller;

import com.kharcha.summary.dto.DashboardResponse;
import com.kharcha.summary.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Monthly financial dashboard metrics and summary")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @Operation(summary = "Get monthly dashboard totals", description = "Returns budget totals, spent amount, days left, and category progress")
    public ResponseEntity<DashboardResponse> getDashboard(
            @AuthenticationPrincipal UUID userId,
            @RequestParam("month") String month
    ) {
        DashboardResponse response = dashboardService.getDashboard(userId, month);
        return ResponseEntity.ok(response);
    }
}
