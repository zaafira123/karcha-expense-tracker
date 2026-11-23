package com.kharcha.transactions.repository;

import com.kharcha.common.domain.Category;
import com.kharcha.transactions.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByUserIdAndOccurredOnBetweenOrderByOccurredOnDescCreatedAtDesc(
            UUID userId, LocalDate startDate, LocalDate endDate);

    List<Transaction> findByUserIdAndCategoryAndOccurredOnBetweenOrderByOccurredOnDescCreatedAtDesc(
            UUID userId, Category category, LocalDate startDate, LocalDate endDate);

    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    List<Transaction> findTop5ByUserIdAndOccurredOnBetweenOrderByOccurredOnDescCreatedAtDesc(
            UUID userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT t.category as category, SUM(t.amountPaise) as totalSpentPaise " +
           "FROM Transaction t " +
           "WHERE t.userId = :userId AND t.occurredOn BETWEEN :startDate AND :endDate " +
           "GROUP BY t.category")
    List<CategorySpendProjection> findCategorySpendByUserIdAndOccurredOnBetween(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(t.amountPaise), 0) FROM Transaction t " +
           "WHERE t.userId = :userId AND t.occurredOn BETWEEN :startDate AND :endDate")
    Long sumAmountPaiseByUserIdAndOccurredOnBetween(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT t.occurredOn AS date, SUM(t.amountPaise) AS totalPaise " +
           "FROM Transaction t " +
           "WHERE t.userId = :userId AND t.occurredOn BETWEEN :startDate AND :endDate " +
           "GROUP BY t.occurredOn")
    List<DailySpendProjection> findDailySpend(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT t.category AS category, t.occurredOn AS date, SUM(t.amountPaise) AS totalPaise " +
           "FROM Transaction t " +
           "WHERE t.userId = :userId AND t.occurredOn BETWEEN :startDate AND :endDate " +
           "GROUP BY t.category, t.occurredOn")
    List<CategoryDailySpendProjection> findCategoryDailySpend(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
