package com.kharcha.parsing.sms;

import com.kharcha.parsing.sms.engine.SmsDuplicateHasher;
import com.kharcha.parsing.sms.engine.SmsMessageType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class SmsDuplicateHasherTest {

    @Test
    void hashesCanonicalTransactionFieldsAndIgnoresMerchantCaseAndSpacing() {
        String first = SmsDuplicateHasher.hash(45_000L, LocalDate.of(2026, 11, 14), "  ZOMATO  ", SmsMessageType.DEBIT);
        String second = SmsDuplicateHasher.hash(45_000L, LocalDate.of(2026, 11, 14), "zomato", SmsMessageType.DEBIT);

        assertThat(first).hasSize(64).isEqualTo(second);
        assertThat(first).matches("[a-f0-9]{64}");
    }

    @Test
    void usesTypeAndAmountInDuplicateIdentity() {
        String debit = SmsDuplicateHasher.hash(45_000L, LocalDate.of(2026, 11, 14), "Zomato", SmsMessageType.DEBIT);
        String credit = SmsDuplicateHasher.hash(45_000L, LocalDate.of(2026, 11, 14), "Zomato", SmsMessageType.CREDIT);
        String otherAmount = SmsDuplicateHasher.hash(45_100L, LocalDate.of(2026, 11, 14), "Zomato", SmsMessageType.DEBIT);

        assertThat(debit).isNotEqualTo(credit).isNotEqualTo(otherAmount);
    }
}
