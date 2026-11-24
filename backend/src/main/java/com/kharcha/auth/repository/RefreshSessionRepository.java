package com.kharcha.auth.repository;

import com.kharcha.auth.domain.RefreshSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface RefreshSessionRepository extends JpaRepository<RefreshSession, UUID> {

    @Modifying
    @Query("delete from RefreshSession session where session.tokenHash = :tokenHash and session.expiresAt > :now")
    int consumeActiveToken(@Param("tokenHash") String tokenHash, @Param("now") Instant now);

    @Modifying
    @Query("delete from RefreshSession session where session.tokenHash = :tokenHash")
    int revokeToken(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("delete from RefreshSession session where session.expiresAt <= :now")
    int deleteExpiredSessions(@Param("now") Instant now);
}
