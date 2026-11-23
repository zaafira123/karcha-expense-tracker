package com.kharcha.agent.repository;

import com.kharcha.agent.domain.AgentProposal;
import com.kharcha.agent.domain.ProposalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgentProposalRepository extends JpaRepository<AgentProposal, UUID> {

    Optional<AgentProposal> findByIdAndUserId(UUID id, UUID userId);

    List<AgentProposal> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, ProposalStatus status);

    List<AgentProposal> findByUserIdAndMonthOrderByCreatedAtDesc(UUID userId, String month);

    List<AgentProposal> findByUserIdAndMonthAndStatusOrderByCreatedAtDesc(UUID userId, String month, ProposalStatus status);

    List<AgentProposal> findByUserIdAndMonthAndStatusIn(UUID userId, String month, Collection<ProposalStatus> statuses);

    List<AgentProposal> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<AgentProposal> findByStatusAndExpiresAtBefore(ProposalStatus status, Instant cutoff);
}
