package com.kharcha.parsing.sms.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SmsParseRequest(
        @NotNull Boolean consentAccepted,
        @NotEmpty @Size(max = 20) List<@NotNull @Size(min = 10, max = 1000) String> messages
) {}
