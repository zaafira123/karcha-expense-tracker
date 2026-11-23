package com.kharcha.parsing.sms.service;

import com.kharcha.common.domain.Category;
import com.kharcha.parsing.sms.domain.SmsImport;
import com.kharcha.parsing.sms.dto.SmsParseDraftResponse;
import com.kharcha.parsing.sms.dto.SmsParseRequest;
import com.kharcha.parsing.sms.dto.SmsParseResponse;
import com.kharcha.parsing.sms.dto.SmsSaveRequest;
import com.kharcha.parsing.sms.dto.SmsSaveResponse;
import com.kharcha.parsing.sms.engine.SmsDuplicateHasher;
import com.kharcha.parsing.sms.engine.SmsMessageDraft;
import com.kharcha.parsing.sms.engine.SmsMessageParser;
import com.kharcha.parsing.sms.engine.SmsMessageType;
import com.kharcha.parsing.sms.repository.SmsImportRepository;
import com.kharcha.transactions.domain.TransactionSource;
import com.kharcha.transactions.dto.CreateTransactionRequest;
import com.kharcha.transactions.service.TransactionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class SmsImportService {

    private final SmsImportRepository smsImportRepository;
    private final TransactionService transactionService;
    private final Clock clock;

    public SmsImportService(SmsImportRepository smsImportRepository, TransactionService transactionService, Clock clock) {
        this.smsImportRepository = smsImportRepository;
        this.transactionService = transactionService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public SmsParseResponse parse(UUID userId, SmsParseRequest request) {
        List<SmsParseDraftResponse> drafts = new ArrayList<>();
        Set<String> batchHashes = new HashSet<>();
        for (int index = 0; index < request.messages().size(); index++) {
            SmsMessageDraft parsed = SmsMessageParser.parse(request.messages().get(index), clock);
            String hash = hashOf(parsed);
            boolean duplicate = parsed.type() == SmsMessageType.DEBIT && hash != null
                    && (batchHashes.contains(hash) || smsImportRepository.existsByUserIdAndMessageHash(userId, hash));
            if (hash != null) batchHashes.add(hash);
            String status;
            String reason = parsed.reason();
            if (duplicate) { status = "DUPLICATE"; reason = "This message matches an expense already imported."; }
            else if (parsed.type() == SmsMessageType.CREDIT) { status = "CREDIT"; reason = "Credits are shown separately and are not saved as expenses."; }
            else if (parsed.type() == SmsMessageType.UNKNOWN) { status = "UNKNOWN"; }
            else if (parsed.needsReview()) { status = "NEEDS_REVIEW"; }
            else { status = "READY"; }
            Category category = parsed.category();
            drafts.add(new SmsParseDraftResponse(index, parsed.type(), parsed.amountPaise(), parsed.merchant(), parsed.occurredOn(),
                    category, category == null ? null : category.getDisplayName(), status, reason, hash, parsed.referenceHint(), parsed.confidence()));
        }
        return new SmsParseResponse(drafts);
    }

    @Transactional
    public SmsSaveResponse save(UUID userId, SmsSaveRequest request) {
        LocalDate today = LocalDate.now(clock);
        Set<String> batchHashes = new HashSet<>();
        List<SmsSaveResponse.SkippedDraft> skipped = new ArrayList<>();
        int savedCount = 0;
        for (SmsSaveRequest.Draft draft : request.drafts()) {
            if (draft.type() != null && !SmsMessageType.DEBIT.name().equals(draft.type())) {
                skipped.add(new SmsSaveResponse.SkippedDraft(draft.index(), "Only debit drafts can be saved."));
                continue;
            }
            if (draft.occurredOn().isAfter(today)) throw new IllegalArgumentException("Expense date cannot be in the future.");
            String expectedHash = SmsDuplicateHasher.hash(draft.amountPaise(), draft.occurredOn(), draft.merchant(), SmsMessageType.DEBIT);
            if (!expectedHash.equalsIgnoreCase(draft.messageHash())) throw new IllegalArgumentException("Draft changed unexpectedly. Read the messages again.");
            if (batchHashes.contains(expectedHash) || smsImportRepository.existsByUserIdAndMessageHash(userId, expectedHash)) {
                skipped.add(new SmsSaveResponse.SkippedDraft(draft.index(), "DUPLICATE"));
                continue;
            }
            batchHashes.add(expectedHash);
            var transaction = transactionService.createTransaction(userId,
                    new CreateTransactionRequest(draft.amountPaise(), draft.category(), draft.merchant().trim(), draft.occurredOn(), null, TransactionSource.SMS));
            smsImportRepository.save(new SmsImport(UUID.randomUUID(), userId, expectedHash, transaction.id(), SmsMessageType.DEBIT,
                    draft.amountPaise(), draft.occurredOn(), Instant.now(clock)));
            savedCount++;
        }
        return new SmsSaveResponse(savedCount, skipped);
    }

    private static String hashOf(SmsMessageDraft draft) {
        if (draft.type() == SmsMessageType.UNKNOWN || draft.amountPaise() == null || draft.merchant() == null || draft.occurredOn() == null) return null;
        return SmsDuplicateHasher.hash(draft.amountPaise(), draft.occurredOn(), draft.merchant(), draft.type());
    }
}
