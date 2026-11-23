package com.kharcha.parsing.sms.engine;

import com.kharcha.common.domain.Category;

import java.time.LocalDate;

public record SmsMessageDraft(
        SmsMessageType type,
        Long amountPaise,
        String merchant,
        LocalDate occurredOn,
        String referenceHint,
        String confidence,
        Category category,
        boolean needsReview,
        String reason
) {}
