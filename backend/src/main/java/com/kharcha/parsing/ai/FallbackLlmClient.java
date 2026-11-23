package com.kharcha.parsing.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Primary
public class FallbackLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(FallbackLlmClient.class);

    private final LlmClient geminiClient;
    private final LlmClient groqClient;

    public FallbackLlmClient(
            @Qualifier("geminiLlmClient") LlmClient geminiClient,
            @Qualifier("groqLlmClient") LlmClient groqClient
    ) {
        this.geminiClient = geminiClient;
        this.groqClient = groqClient;
    }

    @Override
    public Optional<LlmCategoryResult> categorize(String text) {
        try {
            Optional<LlmCategoryResult> geminiResult = geminiClient.categorize(text);
            if (geminiResult.isPresent()) {
                return geminiResult;
            }
        } catch (Exception e) {
            log.warn("Gemini categorization failed; trying Groq fallback");
        }

        try {
            Optional<LlmCategoryResult> groqResult = groqClient.categorize(text);
            if (groqResult.isPresent()) {
                return groqResult;
            }
        } catch (Exception e) {
            log.warn("Groq categorization failed; using rules-only result");
        }

        return Optional.empty();
    }
}
