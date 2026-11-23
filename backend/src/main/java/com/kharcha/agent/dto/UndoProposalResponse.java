package com.kharcha.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response for an undo operation, including whether budgets changed since the move was applied")
public record UndoProposalResponse(
        @Schema(description = "The updated proposal after undo")
        AgentProposalResponse proposal,

        @Schema(description = "True if either budget was manually edited between apply and undo; the undo still happened",
                example = "false")
        boolean budgetsChangedSinceApply
) {}
