package com.kharcha.settings;

import com.kharcha.settings.dto.AgentCapUpdateRequest;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentCapUpdateRequestTest {
    @Test
    void acceptsOnlyTheConfiguredCapRange() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(new AgentCapUpdateRequest(50_000L))).isEmpty();
            assertThat(validator.validate(new AgentCapUpdateRequest(1_000_000L))).isEmpty();
            assertThat(validator.validate(new AgentCapUpdateRequest(49_999L))).isNotEmpty();
            assertThat(validator.validate(new AgentCapUpdateRequest(1_000_001L))).isNotEmpty();
        }
    }
}
