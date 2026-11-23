package com.kharcha.parsing.sms.dto;

import com.kharcha.common.domain.Category;
import com.kharcha.parsing.sms.engine.SmsMessageType;

import java.time.LocalDate;

public record SmsParseDraftResponse(
        int index,
        SmsMessageType type,
        Long amountPaise,
        String merchant,
        LocalDate occurredOn,
        Category category,
        String categoryName,
        String status,
        String reason,
        String messageHash,
        String referenceHint,
        String confidence
) {}
