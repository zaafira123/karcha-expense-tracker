package com.kharcha.parsing.sms;

import com.kharcha.common.domain.Category;
import com.kharcha.parsing.sms.engine.SmsMessageDraft;
import com.kharcha.parsing.sms.engine.SmsMessageParser;
import com.kharcha.parsing.sms.engine.SmsMessageType;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class SmsMessageParserTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-11-16T06:00:00Z"), ZoneId.of("Asia/Kolkata"));

    @Test
    void parsesDebitAmountMerchantDateAndCategory() {
        SmsMessageDraft draft = SmsMessageParser.parse(
                "INR 450.75 has been debited from your account at Zomato on 14-11-26. UPI Ref 928271.", CLOCK);

        assertThat(draft.type()).isEqualTo(SmsMessageType.DEBIT);
        assertThat(draft.amountPaise()).isEqualTo(45_075L);
        assertThat(draft.merchant()).isEqualTo("Zomato");
        assertThat(draft.occurredOn()).isEqualTo(LocalDate.of(2026, 11, 14));
        assertThat(draft.category()).isEqualTo(Category.FOOD_DINING);
        assertThat(draft.needsReview()).isFalse();
    }

    @Test
    void parsesVpaMerchantAndCompactDate() {
        SmsMessageDraft draft = SmsMessageParser.parse(
                "Rs. 1,200 debited to VPA: amazon.pay@okaxis on 14Nov26. Ref 98123", CLOCK);

        assertThat(draft.type()).isEqualTo(SmsMessageType.DEBIT);
        assertThat(draft.amountPaise()).isEqualTo(120_000L);
        assertThat(draft.merchant()).isEqualTo("Amazon Pay");
        assertThat(draft.occurredOn()).isEqualTo(LocalDate.of(2026, 11, 14));
        assertThat(draft.category()).isEqualTo(Category.SHOPPING);
    }

    @Test
    void classifiesCreditsSeparately() {
        SmsMessageDraft draft = SmsMessageParser.parse(
                "Rs 500.00 credited to your account from ACME PAYMENTS on 14/11/2026", CLOCK);

        assertThat(draft.type()).isEqualTo(SmsMessageType.CREDIT);
        assertThat(draft.amountPaise()).isEqualTo(50_000L);
        assertThat(draft.merchant()).isNull();
    }

    @Test
    void marksFutureDatesAndMissingInformationForReview() {
        SmsMessageDraft future = SmsMessageParser.parse(
                "Rs 200 debited at Uber on 30/11/26", CLOCK);
        SmsMessageDraft unclear = SmsMessageParser.parse("Your account notification is ready", CLOCK);

        assertThat(future.needsReview()).isTrue();
        assertThat(unclear.type()).isEqualTo(SmsMessageType.UNKNOWN);
        assertThat(unclear.needsReview()).isTrue();
        assertThat(unclear.reason()).isNotBlank();
    }

    @Test
    void parsesTheRequestedDebitVpaExample() {
        SmsMessageDraft draft = SmsMessageParser.parse(
                "Rs.450.00 debited from A/c XX1234 on 15-11-26 to VPA swiggy@icici", CLOCK);

        assertThat(draft.type()).isEqualTo(SmsMessageType.DEBIT);
        assertThat(draft.amountPaise()).isEqualTo(45_000L);
        assertThat(draft.merchant()).isEqualTo("Swiggy");
        assertThat(draft.occurredOn()).isEqualTo(LocalDate.of(2026, 11, 15));
    }

    @Test
    void parsesCreditCardDebitAndAmazonMerchant() {
        SmsMessageDraft draft = SmsMessageParser.parse(
                "INR 1,250.00 spent on your Credit Card XX5678 at AMAZON on 14Nov26", CLOCK);

        assertThat(draft.type()).isEqualTo(SmsMessageType.DEBIT);
        assertThat(draft.amountPaise()).isEqualTo(125_000L);
        assertThat(draft.merchant()).isEqualTo("Amazon");
        assertThat(draft.occurredOn()).isEqualTo(LocalDate.of(2026, 11, 14));
    }

    @Test
    void keepsCreditsSeparateAndDoesNotInventAnExpenseCategory() {
        SmsMessageDraft draft = SmsMessageParser.parse(
                "Rs 2,000 credited to your account XX1234 on 13-11-26", CLOCK);

        assertThat(draft.type()).isEqualTo(SmsMessageType.CREDIT);
        assertThat(draft.amountPaise()).isEqualTo(200_000L);
        assertThat(draft.category()).isNull();
        assertThat(draft.needsReview()).isFalse();
    }

    @Test
    void parsesCommasAndThreeDateFormatsToTheSameDate() {
        SmsMessageDraft numericDashed = SmsMessageParser.parse(
                "Rs 1,250.00 debited at AMAZON on 14-11-2026", CLOCK);
        SmsMessageDraft numericSlashed = SmsMessageParser.parse(
                "Rs 1,250.00 debited at AMAZON on 14/11/26", CLOCK);
        SmsMessageDraft compact = SmsMessageParser.parse(
                "Rs 1,250.00 debited at AMAZON on 14Nov26", CLOCK);

        assertThat(numericDashed.amountPaise()).isEqualTo(125_000L);
        assertThat(numericDashed.occurredOn()).isEqualTo(LocalDate.of(2026, 11, 14));
        assertThat(numericSlashed.occurredOn()).isEqualTo(numericDashed.occurredOn());
        assertThat(compact.occurredOn()).isEqualTo(numericDashed.occurredOn());
    }

    @Test
    void unknownMessageWithNoAmountIsUnknown() {
        SmsMessageDraft draft = SmsMessageParser.parse(
                "Your account notification is ready", CLOCK);

        assertThat(draft.type()).isEqualTo(SmsMessageType.UNKNOWN);
        assertThat(draft.amountPaise()).isNull();
        assertThat(draft.needsReview()).isTrue();
    }
}
