package com.kharcha.parsing.sms.repository;

import com.kharcha.parsing.sms.domain.SmsImport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SmsImportRepository extends JpaRepository<SmsImport, UUID> {
    boolean existsByUserIdAndMessageHash(UUID userId, String messageHash);
}
