package com.kharcha.parsing.repository;

import com.kharcha.parsing.domain.AiUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiUsageRepository extends JpaRepository<AiUsage, UUID> {

    Optional<AiUsage> findByUserIdAndUsageDate(UUID userId, LocalDate usageDate);
}
