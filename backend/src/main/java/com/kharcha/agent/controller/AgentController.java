package com.kharcha.agent.controller;

import com.kharcha.agent.domain.ProposalStatus;
import com.kharcha.agent.dto.AgentAuditResponse;
import com.kharcha.agent.dto.AgentProposalResponse;
import com.kharcha.agent.dto.AgentWhyResponse;
import com.kharcha.agent.dto.UndoProposalResponse;
import com.kharcha.agent.service.AgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/agent")
@Tag(name = "Budget Agent", description = "Autonomous budget rebalancing proposals and approval workflows")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @GetMapping("/proposals")
    @Operation(summary = "List budget proposals",
            description = "Returns proposals for the authenticated user, newest first. Filter by status or month.")
    public ResponseEntity<List<AgentProposalResponse>> listProposals(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Target month in YYYY-MM format")
            @RequestParam(value = "month", required = false) String month,
            @Parameter(description = "Filter by proposal status")
            @RequestParam(value = "status", required = false) ProposalStatus status
    ) {
        return ResponseEntity.ok(agentService.listProposals(userId, month, status));
    }

    @PostMapping("/proposals/run")
    @Operation(summary = "Generate budget proposals",
            description = "Evaluates the monthly forecast and creates PENDING proposals for shortfalls")
    public ResponseEntity<List<AgentProposalResponse>> runProposals(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Target month in YYYY-MM format", required = true)
            @RequestParam("month") String month
    ) {
        return ResponseEntity.ok(agentService.runProposalsFor(userId, month));
    }

    @PostMapping("/proposals/{id}/approve")
    @Operation(summary = "Approve budget proposal",
            description = "Applies the proposed rebalancing amounts to category budgets. Returns 409 if not PENDING or if expired.")
    public ResponseEntity<AgentProposalResponse> approveProposal(
            @AuthenticationPrincipal UUID userId,
            @PathVariable("id") UUID id
    ) {
        return ResponseEntity.ok(agentService.applyProposal(userId, id));
    }

    @PostMapping("/proposals/{id}/reject")
    @Operation(summary = "Reject budget proposal",
            description = "Marks the proposal REJECTED without changing budgets. Returns 409 if not PENDING.")
    public ResponseEntity<AgentProposalResponse> rejectProposal(
            @AuthenticationPrincipal UUID userId,
            @PathVariable("id") UUID id
    ) {
        return ResponseEntity.ok(agentService.rejectProposal(userId, id));
    }

    @PostMapping("/proposals/{id}/undo")
    @Operation(summary = "Undo a budget move",
            description = "Reverses an APPLIED budget move within 7 days. Returns 409 UNDO_WINDOW_CLOSED after 7 days. " +
                          "Response includes budgetsChangedSinceApply flag.")
    public ResponseEntity<UndoProposalResponse> undoProposal(
            @AuthenticationPrincipal UUID userId,
            @PathVariable("id") UUID id
    ) {
        return ResponseEntity.ok(agentService.undoProposal(userId, id));
    }

    @GetMapping("/proposals/{id}/why")
    @Operation(summary = "Explain a proposal",
            description = "Returns a template explanation of why the proposal was generated, built from stored numbers. " +
                          "An AI rewrite hook is available in AgentService.buildWhyTemplate() for future enhancement.")
    public ResponseEntity<AgentWhyResponse> getProposalWhy(
            @AuthenticationPrincipal UUID userId,
            @PathVariable("id") UUID id
    ) {
        return ResponseEntity.ok(agentService.getProposalWhy(userId, id));
    }

    @GetMapping("/audit")
    @Operation(summary = "List agent audit log",
            description = "Returns the complete history of agent events for the authenticated user, newest first")
    public ResponseEntity<List<AgentAuditResponse>> listAudit(
            @AuthenticationPrincipal UUID userId
    ) {
        return ResponseEntity.ok(agentService.listAuditLog(userId));
    }
}
