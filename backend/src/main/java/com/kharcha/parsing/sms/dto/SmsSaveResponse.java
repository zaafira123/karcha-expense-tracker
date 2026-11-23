package com.kharcha.parsing.sms.dto;

import java.util.List;

public record SmsSaveResponse(int savedCount, List<SkippedDraft> skipped) {
    public record SkippedDraft(int index, String reason) {}
}
