package com.kharcha.settings.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AgentCapUpdateRequest(
        @NotNull @Min(50_000) @Max(1_000_000) Long amountPaise
) {}
