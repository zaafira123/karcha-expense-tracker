package com.kharcha.agent.dto;

import com.kharcha.agent.domain.AgentAudit;
import com.kharcha.agent.domain.AgentAuditEventType;
import com.kharcha.common.util.PaiseFormatter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Schema(description = "Agent audit log entry")
public record AgentAuditResponse(
        @Schema(description = "Unique audit entry ID")
        UUID id,

        @Schema(description = "Associated proposal ID, if applicable")
        UUID proposalId,

        @Schema(description = "Event type", example = "PROPOSED")
        AgentAuditEventType event,

        @Schema(description = "Event details or payload")
        String detail,

        @Schema(description = "Event timestamp")
        Instant createdAt
) {
    private static final Pattern LEGACY_PAISE = Pattern.compile("(\\d+) paise");
    private static final Pattern LEGACY_BALANCE = Pattern.compile("(before=|after=)(\\d+)");

    public static AgentAuditResponse from(AgentAudit audit) {
        return new AgentAuditResponse(
                audit.getId(),
                audit.getProposalId(),
                audit.getEvent(),
                displayDetail(audit.getDetail()),
                audit.getCreatedAt()
        );
    }

    private static String displayDetail(String detail) {
        if (detail == null) return null;
        Matcher paise = LEGACY_PAISE.matcher(detail);
        String formatted = paise.replaceAll(match -> Matcher.quoteReplacement(PaiseFormatter.format(Long.parseLong(match.group(1)))));
        Matcher balances = LEGACY_BALANCE.matcher(formatted);
        return balances.replaceAll(match -> Matcher.quoteReplacement(match.group(1)
                + PaiseFormatter.format(Long.parseLong(match.group(2)))));
    }
}
