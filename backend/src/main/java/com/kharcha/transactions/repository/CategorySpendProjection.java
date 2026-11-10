package com.kharcha.transactions.repository;

import com.kharcha.common.domain.Category;

public interface CategorySpendProjection {
    Category getCategory();
    Long getTotalSpentPaise();
}
