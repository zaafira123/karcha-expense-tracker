package com.kharcha.budgets.repository;

import com.kharcha.budgets.domain.Budget;
import com.kharcha.common.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    List<Budget> findByUserIdAndMonth(UUID userId, String month);

    Optional<Budget> findByUserIdAndCategoryAndMonth(UUID userId, Category category, String month);
}
