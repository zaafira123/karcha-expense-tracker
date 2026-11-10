package com.kharcha.common.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

public final class MonthUtils {

    private static final Pattern MONTH_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

    private MonthUtils() {}

    public static YearMonth parseMonth(String month) {
        if (month == null || !MONTH_PATTERN.matcher(month.trim()).matches()) {
            throw new IllegalArgumentException("Invalid month format. Expected yyyy-MM (e.g. 2026-10)");
        }
        try {
            return YearMonth.parse(month.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid month format. Expected yyyy-MM (e.g. 2026-10)");
        }
    }

    public static LocalDate getStartDate(YearMonth yearMonth) {
        return yearMonth.atDay(1);
    }

    public static LocalDate getEndDate(YearMonth yearMonth) {
        return yearMonth.atEndOfMonth();
    }
}
