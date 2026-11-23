package com.kharcha.parsing;

import com.kharcha.parsing.engine.AmountParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AmountParserTest {

    @Test
    @DisplayName("Parses plain integer amounts to paise")
    void testPlainAmount() {
        assertThat(AmountParser.parseAmountPaise("paid 450 for pizza")).isEqualTo(45000L);
    }

    @Test
    @DisplayName("Parses amounts with commas")
    void testCommaAmount() {
        assertThat(AmountParser.parseAmountPaise("spent 1,200 at dmart")).isEqualTo(120000L);
    }

    @Test
    @DisplayName("Parses amounts with decimals")
    void testDecimalAmount() {
        assertThat(AmountParser.parseAmountPaise("order 1200.50 for groceries")).isEqualTo(120050L);
        assertThat(AmountParser.parseAmountPaise("coffee 150.05")).isEqualTo(15005L);
        assertThat(AmountParser.parseAmountPaise("snack 20.5")).isEqualTo(2050L);
    }

    @Test
    @DisplayName("Parses amounts with k suffix (thousands)")
    void testKSuffix() {
        assertThat(AmountParser.parseAmountPaise("spent 1.5k on clothes")).isEqualTo(150000L);
        assertThat(AmountParser.parseAmountPaise("fuel 2k")).isEqualTo(200000L);
        assertThat(AmountParser.parseAmountPaise("flight 4.25k")).isEqualTo(425000L);
    }

    @Test
    @DisplayName("Parses amounts with currency prefixes and suffixes")
    void testCurrencyMarkers() {
        assertThat(AmountParser.parseAmountPaise("rs 450 for food")).isEqualTo(45000L);
        assertThat(AmountParser.parseAmountPaise("rs. 300 for medicine")).isEqualTo(30000L);
        assertThat(AmountParser.parseAmountPaise("paid ₹500 for petrol")).isEqualTo(50000L);
        assertThat(AmountParser.parseAmountPaise("450 rs for burger")).isEqualTo(45000L);
        assertThat(AmountParser.parseAmountPaise("600 rupees for ticket")).isEqualTo(60000L);
    }

    @Test
    @DisplayName("Ignores numbers inside date expressions")
    void testSkipsDateNumbers() {
        assertThat(AmountParser.parseAmountPaise("on 5 nov spent 450 at dmart")).isEqualTo(45000L);
        assertThat(AmountParser.parseAmountPaise("paid 1,200 on 5/11")).isEqualTo(120000L);
    }

    @Test
    @DisplayName("Returns null when no amount is present")
    void testNoAmount() {
        assertThat(AmountParser.parseAmountPaise("dinner with friends")).isNull();
        assertThat(AmountParser.parseAmountPaise("")).isNull();
        assertThat(AmountParser.parseAmountPaise(null)).isNull();
    }
}
