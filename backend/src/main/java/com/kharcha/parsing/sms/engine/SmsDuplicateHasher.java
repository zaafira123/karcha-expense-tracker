package com.kharcha.parsing.sms.engine;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Locale;

public final class SmsDuplicateHasher {

    private SmsDuplicateHasher() {}

    public static String hash(long amountPaise, LocalDate occurredOn, String merchant, SmsMessageType type) {
        String canonical = amountPaise + "|" + occurredOn + "|" + normalizeMerchant(merchant) + "|" + type.name();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    public static String normalizeMerchant(String merchant) {
        if (merchant == null) return "";
        return merchant.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
