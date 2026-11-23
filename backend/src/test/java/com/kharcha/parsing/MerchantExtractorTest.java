package com.kharcha.parsing;

import com.kharcha.parsing.engine.MerchantExtractor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MerchantExtractorTest {

    @Test
    @DisplayName("Extracts merchant after preposition 'for'")
    void testForPreposition() {
        String merchant = MerchantExtractor.extractMerchant("paid 450 for pizza with friends", "pizza");
        assertThat(merchant).isEqualTo("Pizza with friends");
    }

    @Test
    @DisplayName("Extracts merchant after preposition 'at' before date word")
    void testAtPreposition() {
        String merchant = MerchantExtractor.extractMerchant("spent 1,200 at dmart yesterday", "dmart");
        assertThat(merchant).isEqualTo("Dmart");
    }

    @Test
    @DisplayName("Extracts merchant after preposition 'on' before date word")
    void testOnPreposition() {
        String merchant = MerchantExtractor.extractMerchant("spent 1.5k on myntra yesterday", "myntra");
        assertThat(merchant).isEqualTo("Myntra");
    }

    @Test
    @DisplayName("Falls back to matched keyword when no preposition is present")
    void testKeywordFallback() {
        String merchant = MerchantExtractor.extractMerchant("swiggy 280", "swiggy");
        assertThat(merchant).isEqualTo("Swiggy");
    }

    @Test
    @DisplayName("Returns null when no preposition and no keyword are present")
    void testNullMerchant() {
        String merchant = MerchantExtractor.extractMerchant("paid 450", null);
        assertThat(merchant).isNull();
    }
}
