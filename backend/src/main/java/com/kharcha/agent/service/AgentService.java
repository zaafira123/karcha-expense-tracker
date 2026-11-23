package com.kharcha.agent.service;

import com.kharcha.agent.domain.AgentAudit;
import com.kharcha.agent.domain.AgentAuditEventType;
import com.kharcha.agent.domain.AgentDecisionEngine;
import com.kharcha.agent.domain.AgentProposal;
import com.kharcha.agent.domain.CategoryProjection;
import com.kharcha.agent.domain.ProposalStatus;
import com.kharcha.agent.domain.ProposedMove;
import com.kharcha.agent.dto.AgentAuditResponse;
import com.kharcha.agent.dto.AgentProposalResponse;
import com.kharcha.agent.dto.AgentWhyResponse;
import com.kharcha.agent.dto.UndoProposalResponse;
import com.kharcha.agent.repository.AgentAuditRepository;
import com.kharcha.agent.repository.AgentProposalRepository;
import com.kharcha.auth.domain.User;
import com.kharcha.auth.repository.UserRepository;
import com.kharcha.budgets.domain.Budget;
import com.kharcha.budgets.repository.BudgetRepository;
import com.kharcha.common.domain.Category;
import com.kharcha.common.error.ProposalExpiredException;
import com.kharcha.common.error.ResourceNotFoundException;
import com.kharcha.common.error.UndoWindowClosedException;
import com.kharcha.common.util.MonthUtils;
import com.kharcha.common.util.PaiseFormatter;
import com.kharcha.forecast.domain.ForecastStatus;
import com.kharcha.forecast.dto.CategoryForecastResponse;
import com.kharcha.forecast.dto.ForecastResponse;
import com.kharcha.forecast.service.ForecastService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final AgentProposalRepository proposalRepository;
    private final AgentAuditRepository auditRepository;
    private final ForecastService forecastService;
    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public AgentService(
            AgentProposalRepository proposalRepository,
            AgentAuditRepository auditRepository,
            ForecastService forecastService,
            BudgetRepository budgetRepository,
            UserRepository userRepository,
            Clock clock
    ) {
        this.proposalRepository = proposalRepository;
        this.auditRepository = auditRepository;
        this.forecastService = forecastService;
        this.budgetRepository = budgetRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    // -------------------------------------------------------------------------
    // Run proposals
    // -------------------------------------------------------------------------

    /**
     * Runs the agent for one user and month.
     * <ol>
     *   <li>Loads the forecast (reuses ForecastService — no duplicated calculation).</li>
     *   <li>Returns empty list if forecast status is INSUFFICIENT_DATA.</li>
     *   <li>Expires PENDING proposals older than 7 days (writes audit row).</li>
     *   <li>Skips categories that already have a PENDING proposal for this month.</li>
     *   <li>Runs the pure decision engine.</li>
     *   <li>Saves each new proposal as PENDING and writes an audit row (PROPOSED).</li>
     * </ol>
     */
    @Transactional
    public List<AgentProposalResponse> runProposalsFor(UUID userId, String monthStr) {
        // 1. Load forecast — reuse, never duplicate.
        ForecastResponse forecast = forecastService.getForecast(userId, monthStr);

        // 2. Insufficient data → no proposals.
        if (forecast.status() == ForecastStatus.INSUFFICIENT_DATA) {
            return List.of();
        }

        // 3. Expire stale PENDING proposals (any month).
        expireStalePendingProposals(userId);

        // 4. Skip receivers already covered by an active PENDING proposal this month.
        List<AgentProposal> existingPending = proposalRepository
                .findByUserIdAndMonthAndStatusOrderByCreatedAtDesc(userId, monthStr, ProposalStatus.PENDING);

        Set<Category> excludedReceivers = existingPending.stream()
                .map(AgentProposal::getToCategory)
                .collect(Collectors.toSet());

        // 5. Load user's agent move cap.
        long capPaise = userRepository.findById(userId)
                .map(User::getAgentCapPaise)
                .orElse(200000L);

        // Map forecast per-category projections.
        List<CategoryProjection> projections = new ArrayList<>();
        for (CategoryForecastResponse catResp : forecast.categories()) {
            Category cat = Category.valueOf(catResp.category());
            projections.add(new CategoryProjection(
                    cat,
                    catResp.budgetPaise(),
                    catResp.projectedPaise(),
                    cat.isProtected()
            ));
        }

        // 6. Pure decision logic — no DB, no HTTP, no LLM.
        List<ProposedMove> moves = AgentDecisionEngine.generateProposals(
                projections,
                capPaise,
                2,
                excludedReceivers
        );

        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(7, ChronoUnit.DAYS);
        List<AgentProposalResponse> savedProposals = new ArrayList<>();

        // 7. Persist each proposal and write PROPOSED audit row.
        for (ProposedMove move : moves) {
            UUID proposalId = UUID.randomUUID();
            AgentProposal proposal = new AgentProposal(
                    proposalId,
                    userId,
                    monthStr,
                    move.fromCategory(),
                    move.toCategory(),
                    move.amountPaise(),
                    move.shortfallPaise(),
                    move.donorSparePaise(),
                    ProposalStatus.PENDING,
                    now,
                    null,
                    null,
                    expiresAt
            );

            proposalRepository.save(proposal);

            String detail = String.format(
                    "Proposed rebalancing move of %s from %s to %s",
                    PaiseFormatter.format(move.amountPaise()),
                    move.fromCategory().getDisplayName(),
                    move.toCategory().getDisplayName()
            );
            writeAudit(userId, proposalId, AgentAuditEventType.PROPOSED, detail, now);

            savedProposals.add(AgentProposalResponse.from(proposal));
        }

        return savedProposals;
    }

    // -------------------------------------------------------------------------
    // Approve / apply
    // -------------------------------------------------------------------------

    /**
     * Approves a PENDING proposal: validates state and expiry, applies budget
     * changes in one transaction, records before/after snapshots in the proposal
     * for undo detection, and writes an APPLIED audit row.
     */
    @Transactional
    public AgentProposalResponse applyProposal(UUID userId, UUID proposalId) {
        AgentProposal proposal = proposalRepository.findByIdAndUserId(proposalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));

        // Only PENDING proposals can be approved → 409.
        if (proposal.getStatus() != ProposalStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending proposals can be approved. Current status: " + proposal.getStatus());
        }

        Instant now = Instant.now(clock);

        // Expired proposals → 409 PROPOSAL_EXPIRED.
        if (now.isAfter(proposal.getExpiresAt())) {
            proposal.setStatus(ProposalStatus.EXPIRED);
            proposalRepository.save(proposal);
            writeAudit(userId, proposalId, AgentAuditEventType.EXPIRED,
                    "Proposal expired before approval", now);
            throw new ProposalExpiredException(
                    "This proposal expired on " + proposal.getExpiresAt() + " and can no longer be approved");
        }

        String month = proposal.getMonth();
        long moveAmount = proposal.getAmountPaise();

        Budget donorBudget = budgetRepository
                .findByUserIdAndCategoryAndMonth(userId, proposal.getFromCategory(), month)
                .orElseGet(() -> new Budget(UUID.randomUUID(), userId, proposal.getFromCategory(), month, 0L));

        Budget receiverBudget = budgetRepository
                .findByUserIdAndCategoryAndMonth(userId, proposal.getToCategory(), month)
                .orElseGet(() -> new Budget(UUID.randomUUID(), userId, proposal.getToCategory(), month, 0L));

        // Capture before-values for undo detection.
        long donorBefore = donorBudget.getAmountPaise();
        long receiverBefore = receiverBudget.getAmountPaise();

        donorBudget.setAmountPaise(Math.max(0L, donorBefore - moveAmount));
        receiverBudget.setAmountPaise(receiverBefore + moveAmount);

        budgetRepository.save(donorBudget);
        budgetRepository.save(receiverBudget);

        // Persist snapshot so undo can detect drift.
        proposal.setDonorBudgetBeforePaise(donorBefore);
        proposal.setReceiverBudgetBeforePaise(receiverBefore);
        proposal.setStatus(ProposalStatus.APPLIED);
        proposal.setDecidedAt(now);
        proposal.setAppliedAt(now);
        AgentProposal saved = proposalRepository.save(proposal);

        String detail = String.format(
                "Applied budget move of %s from %s (before %s, after %s) to %s (before %s, after %s)",
                PaiseFormatter.format(moveAmount),
                proposal.getFromCategory().getDisplayName(), PaiseFormatter.format(donorBefore), PaiseFormatter.format(donorBudget.getAmountPaise()),
                proposal.getToCategory().getDisplayName(), PaiseFormatter.format(receiverBefore), PaiseFormatter.format(receiverBudget.getAmountPaise())
        );
        writeAudit(userId, proposalId, AgentAuditEventType.APPLIED, detail, now);

        return AgentProposalResponse.from(saved);
    }

    // -------------------------------------------------------------------------
    // Reject
    // -------------------------------------------------------------------------

    /**
     * Rejects a PENDING proposal → 409 if not PENDING.
     */
    @Transactional
    public AgentProposalResponse rejectProposal(UUID userId, UUID proposalId) {
        AgentProposal proposal = proposalRepository.findByIdAndUserId(proposalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));

        if (proposal.getStatus() != ProposalStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending proposals can be rejected. Current status: " + proposal.getStatus());
        }

        Instant now = Instant.now(clock);
        proposal.setStatus(ProposalStatus.REJECTED);
        proposal.setDecidedAt(now);
        AgentProposal saved = proposalRepository.save(proposal);

        writeAudit(userId, proposalId, AgentAuditEventType.REJECTED, "Proposal rejected by user", now);
        return AgentProposalResponse.from(saved);
    }

    // -------------------------------------------------------------------------
    // Undo
    // -------------------------------------------------------------------------

    /**
     * Undoes an APPLIED proposal within 7 days of apply.
     * <ul>
     *   <li>Non-APPLIED → 409.</li>
     *   <li>Outside 7-day window → 409 UNDO_WINDOW_CLOSED.</li>
     *   <li>Reverses budget changes. If either budget drifted from the snapshot,
     *       {@code budgetsChangedSinceApply} is {@code true} in the response.</li>
     * </ul>
     */
    @Transactional
    public UndoProposalResponse undoProposal(UUID userId, UUID proposalId) {
        AgentProposal proposal = proposalRepository.findByIdAndUserId(proposalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));

        if (proposal.getStatus() != ProposalStatus.APPLIED) {
            throw new IllegalStateException(
                    "Only applied proposals can be undone. Current status: " + proposal.getStatus());
        }

        Instant now = Instant.now(clock);

        if (proposal.getAppliedAt() == null
                || now.isAfter(proposal.getAppliedAt().plus(7, ChronoUnit.DAYS))) {
            throw new UndoWindowClosedException(
                    "The 7-day undo window has closed. Applied at: " + proposal.getAppliedAt());
        }

        String month = proposal.getMonth();
        long moveAmount = proposal.getAmountPaise();

        Budget donorBudget = budgetRepository
                .findByUserIdAndCategoryAndMonth(userId, proposal.getFromCategory(), month)
                .orElseGet(() -> new Budget(UUID.randomUUID(), userId, proposal.getFromCategory(), month, 0L));

        Budget receiverBudget = budgetRepository
                .findByUserIdAndCategoryAndMonth(userId, proposal.getToCategory(), month)
                .orElseGet(() -> new Budget(UUID.randomUUID(), userId, proposal.getToCategory(), month, 0L));

        // Detect budget drift using snapshots captured at apply time.
        boolean budgetsChangedSinceApply = false;
        if (proposal.getDonorBudgetBeforePaise() != null && proposal.getReceiverBudgetBeforePaise() != null) {
            long expectedDonorNow = Math.max(0L, proposal.getDonorBudgetBeforePaise() - moveAmount);
            long expectedReceiverNow = proposal.getReceiverBudgetBeforePaise() + moveAmount;
            if (donorBudget.getAmountPaise() != expectedDonorNow
                    || receiverBudget.getAmountPaise() != expectedReceiverNow) {
                budgetsChangedSinceApply = true;
            }
        }

        // Always reverse regardless of drift.
        donorBudget.setAmountPaise(donorBudget.getAmountPaise() + moveAmount);
        receiverBudget.setAmountPaise(Math.max(0L, receiverBudget.getAmountPaise() - moveAmount));

        budgetRepository.save(donorBudget);
        budgetRepository.save(receiverBudget);

        proposal.setStatus(ProposalStatus.UNDONE);
        AgentProposal saved = proposalRepository.save(proposal);

        String detail = String.format(
                "Undid budget move of %s: %s restored to %s, %s restored to %s%s",
                PaiseFormatter.format(moveAmount),
                proposal.getFromCategory().getDisplayName(), PaiseFormatter.format(donorBudget.getAmountPaise()),
                proposal.getToCategory().getDisplayName(), PaiseFormatter.format(receiverBudget.getAmountPaise()),
                budgetsChangedSinceApply ? " [budgets had drifted since apply]" : ""
        );
        writeAudit(userId, proposalId, AgentAuditEventType.UNDONE, detail, now);

        return new UndoProposalResponse(AgentProposalResponse.from(saved), budgetsChangedSinceApply);
    }

    // -------------------------------------------------------------------------
    // List
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<AgentProposalResponse> listProposals(UUID userId, String monthStr, ProposalStatus status) {
        List<AgentProposal> proposals;
        if (monthStr != null && !monthStr.isBlank() && status != null) {
            MonthUtils.parseMonth(monthStr);
            proposals = proposalRepository.findByUserIdAndMonthAndStatusOrderByCreatedAtDesc(userId, monthStr, status);
        } else if (monthStr != null && !monthStr.isBlank()) {
            MonthUtils.parseMonth(monthStr);
            proposals = proposalRepository.findByUserIdAndMonthOrderByCreatedAtDesc(userId, monthStr);
        } else if (status != null) {
            proposals = proposalRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status);
        } else {
            proposals = proposalRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }
        return proposals.stream().map(AgentProposalResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AgentAuditResponse> listAuditLog(UUID userId) {
        return auditRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(AgentAuditResponse::from)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Why
    // -------------------------------------------------------------------------

    /**
     * Returns a template explanation for a proposal built from the stored numbers.
     * <p>
     * <b>AI rewrite hook</b>: replace the returned string with an AI-generated
     * version by calling {@code LlmClient} here when the AI rewrite feature is added.
     * Never let the LLM change the numbers — only the phrasing.
     * </p>
     */
    @Transactional(readOnly = true)
    public AgentWhyResponse getProposalWhy(UUID userId, UUID proposalId) {
        AgentProposal proposal = proposalRepository.findByIdAndUserId(proposalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));

        String explanation = buildWhyTemplate(proposal);
        return new AgentWhyResponse(proposalId, explanation);
    }

    private String buildWhyTemplate(AgentProposal proposal) {

        // Receiver projected = budget + shortfall (we stored shortfall, not projected directly).
        // Donor projected is approximated: budget - spare = ceil(1.25 * projected)
        // We display what we know precisely from stored fields.
        String receiverName = proposal.getToCategory().getDisplayName();
        String donorName    = proposal.getFromCategory().getDisplayName();

        return String.format(
                "%s is projected to exceed its budget by %s. " +
                "%s has %s spare above the 80%% safety level. " +
                "Moving %s covers the shortfall while keeping %s above the safety level.",
                receiverName, PaiseFormatter.format(proposal.getShortfallPaise()),
                donorName, PaiseFormatter.format(proposal.getDonorSparePaise()),
                PaiseFormatter.format(proposal.getAmountPaise()),
                donorName
        );
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void expireStalePendingProposals(UUID userId) {
        Instant now = Instant.now(clock);
        List<AgentProposal> stale = proposalRepository.findByStatusAndExpiresAtBefore(ProposalStatus.PENDING, now);
        for (AgentProposal p : stale) {
            if (p.getUserId().equals(userId)) {
                p.setStatus(ProposalStatus.EXPIRED);
                proposalRepository.save(p);
                writeAudit(userId, p.getId(), AgentAuditEventType.EXPIRED,
                        "Proposal expired after 7 days", now);
            }
        }
    }

    private void writeAudit(UUID userId, UUID proposalId, AgentAuditEventType event,
                            String detail, Instant timestamp) {
        AgentAudit audit = new AgentAudit(
                UUID.randomUUID(),
                userId,
                proposalId,
                event,
                detail,
                timestamp
        );
        auditRepository.save(audit);
    }
}
