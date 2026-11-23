package com.kharcha.transactions.repository;

import com.kharcha.common.domain.Category;

import java.time.LocalDate;

public interface CategoryDailySpendProjection {
    Category getCategory();
    LocalDate getDate();
    Long getTotalPaise();
}
