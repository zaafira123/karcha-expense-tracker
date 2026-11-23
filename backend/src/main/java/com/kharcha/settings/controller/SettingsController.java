package com.kharcha.settings.controller;

import com.kharcha.settings.dto.AgentCapUpdateRequest;
import com.kharcha.settings.dto.AgentCapUpdateResponse;
import com.kharcha.settings.dto.SettingsResponse;
import com.kharcha.settings.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/settings")
@Tag(name = "Settings", description = "Account preferences")
public class SettingsController {
    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    @Operation(summary = "Get account settings")
    public ResponseEntity<SettingsResponse> getSettings(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(settingsService.getSettings(userId));
    }

    @PutMapping("/agent-cap")
    @Operation(summary = "Set maximum agent move")
    public ResponseEntity<AgentCapUpdateResponse> updateAgentCap(@AuthenticationPrincipal UUID userId,
                                                                  @Valid @RequestBody AgentCapUpdateRequest request) {
        return ResponseEntity.ok(settingsService.updateAgentCap(userId, request.amountPaise()));
    }
}
