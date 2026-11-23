package com.kharcha.parsing.sms.dto;

import java.util.List;

public record SmsParseResponse(List<SmsParseDraftResponse> drafts) {}
