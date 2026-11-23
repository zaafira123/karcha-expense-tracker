package com.kharcha.transactions.repository;

import java.time.LocalDate;

public interface DailySpendProjection {
    LocalDate getDate();
    Long getTotalPaise();
}
