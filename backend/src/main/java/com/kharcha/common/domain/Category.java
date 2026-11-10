package com.kharcha.common.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

public enum Category {
    FOOD_DINING("Food and dining", false),
    GROCERIES("Groceries", false),
    TRAVEL("Travel", false),
    SHOPPING("Shopping", false),
    BILLS_UTILITIES("Bills and utilities", true),
    HEALTH("Health", true),
    ENTERTAINMENT("Entertainment", false),
    SAVINGS("Savings", true);

    private final String displayName;
    private final boolean isProtected;

    Category(String displayName, boolean isProtected) {
        this.displayName = displayName;
        this.isProtected = isProtected;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isProtected() {
        return isProtected;
    }

    public CategoryInfo toInfo() {
        return new CategoryInfo(this.name(), this.displayName, this.isProtected);
    }

    public record CategoryInfo(
            String code,
            String displayName,
            boolean isProtected
    ) {}
}
