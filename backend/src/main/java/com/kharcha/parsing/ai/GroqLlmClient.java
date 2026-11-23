package com.kharcha.parsing.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kharcha.common.domain.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component("groqLlmClient")
public class GroqLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(GroqLlmClient.class);

    private final String apiKey;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GroqLlmClient(
            @Value("${ai.groq-api-key:}") String apiKey,
            ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    @Override
    public Optional<LlmCategoryResult> categorize(String text) {
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }

        // 1 initial attempt + 1 retry = at most 2 attempts
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                return executeGroqCall(text);
            } catch (Exception e) {
                log.warn("Groq call attempt {} failed; categorization will use the next fallback", attempt);
                if (attempt == 2) {
                    return Optional.empty();
                }
            }
        }

        return Optional.empty();
    }

    private Optional<LlmCategoryResult> executeGroqCall(String text) throws Exception {
        String url = "https://api.groq.com/openai/v1/chat/completions";

        Map<String, Object> requestBody = Map.of(
                "model", "llama-3.1-8b-instant",
                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content", "You are an expense categorizer. Output JSON strictly with shape: {\"category\": <one of FOOD_DINING, GROCERIES, TRAVEL, SHOPPING, BILLS_UTILITIES, HEALTH, ENTERTAINMENT, SAVINGS, or null>, \"merchant\": <string or null>}."
                        ),
                        Map.of(
                                "role", "user",
                                "content", text
                        )
                ),
                "response_format", Map.of("type", "json_object")
        );

        String responseBody = restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        if (responseBody == null || responseBody.isBlank()) {
            return Optional.empty();
        }

        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode choices = root.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            String content = choices.get(0).path("message").path("content").asText();
            JsonNode resultJson = objectMapper.readTree(content);

            String catStr = resultJson.path("category").asText(null);
            String merchant = resultJson.path("merchant").asText(null);
            if ("null".equalsIgnoreCase(merchant)) {
                merchant = null;
            }

            Category category = null;
            if (catStr != null && !catStr.isBlank() && !"null".equalsIgnoreCase(catStr)) {
                try {
                    category = Category.valueOf(catStr.toUpperCase());
                } catch (IllegalArgumentException ignored) {
                    // Invalid category name -> treat as null
                }
            }

            if (category != null || merchant != null) {
                return Optional.of(new LlmCategoryResult(category, merchant));
            }
        }

        return Optional.empty();
    }
}
