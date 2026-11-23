package com.kharcha.parsing.sms.dto;

import com.kharcha.common.domain.Category;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record SmsSaveRequest(
        @NotNull Boolean consentAccepted,
        @NotEmpty @Size(max = 20) List<@Valid Draft> drafts
) {
    public record Draft(
            @PositiveOrZero int index,
            @NotNull @Positive Long amountPaise,
            @NotNull Category category,
            @NotBlank @Size(max = 120) String merchant,
            @NotNull LocalDate occurredOn,
            @NotBlank @Pattern(regexp = "[a-fA-F0-9]{64}") String messageHash,
            String type
    ) {}

}
