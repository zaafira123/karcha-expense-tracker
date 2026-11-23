package com.kharcha.parsing.sms.controller;

import com.kharcha.common.error.ErrorResponse;
import com.kharcha.parsing.sms.dto.SmsParseRequest;
import com.kharcha.parsing.sms.dto.SmsParseResponse;
import com.kharcha.parsing.sms.dto.SmsSaveRequest;
import com.kharcha.parsing.sms.dto.SmsSaveResponse;
import com.kharcha.parsing.sms.service.SmsImportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/imports/sms")
public class SmsImportController {

    private final SmsImportService smsImportService;

    public SmsImportController(SmsImportService smsImportService) {
        this.smsImportService = smsImportService;
    }

    @PostMapping("/parse")
    public ResponseEntity<?> parse(@AuthenticationPrincipal UUID userId, @Valid @RequestBody SmsParseRequest request) {
        if (!Boolean.TRUE.equals(request.consentAccepted())) return consentRequired();
        SmsParseResponse response = smsImportService.parse(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/save")
    public ResponseEntity<?> save(@AuthenticationPrincipal UUID userId, @Valid @RequestBody SmsSaveRequest request) {
        if (!Boolean.TRUE.equals(request.consentAccepted())) return consentRequired();
        SmsSaveResponse response = smsImportService.save(userId, request);
        return ResponseEntity.ok(response);
    }

    private static ResponseEntity<ErrorResponse> consentRequired() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("CONSENT_REQUIRED", "Confirm how message text is processed before importing.", Map.of()));
    }
}
