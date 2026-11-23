package com.kharcha.agent.repository;

import com.kharcha.agent.domain.AgentAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AgentAuditRepository extends JpaRepository<AgentAudit, UUID> {

    List<AgentAudit> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
