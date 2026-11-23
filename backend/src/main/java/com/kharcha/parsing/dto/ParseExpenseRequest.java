package com.kharcha.parsing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to parse an expense sentence")
public record ParseExpenseRequest(
        @NotBlank(message = "Sentence text is required")
        @Size(min = 1, max = 200, message = "Sentence must be between 1 and 200 characters")
        @Schema(description = "Single natural language expense sentence", example = "paid 450 for pizza with friends")
        String text
) {}
