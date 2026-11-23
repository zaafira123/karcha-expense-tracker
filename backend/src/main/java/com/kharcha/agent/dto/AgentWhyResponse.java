package com.kharcha.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Human-readable explanation of why a proposal was generated")
public record AgentWhyResponse(
        @Schema(description = "The proposal ID this explanation relates to")
        UUID proposalId,

        @Schema(description = "Template explanation built from the proposal numbers",
                example = "Food and dining is projected to spend ₹7,800 against a budget of ₹6,000. " +
                           "Groceries is projected to spend ₹4,000 against ₹8,000, leaving ₹3,000 spare. " +
                           "Moving ₹1,800 keeps Groceries above the 80% safety level.")
        String explanation
) {}
