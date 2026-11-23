package com.kharcha.parsing.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kharcha.common.domain.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component("geminiLlmClient")
public class GeminiLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiLlmClient.class);

    private final String apiKey;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GeminiLlmClient(
            @Value("${ai.gemini-api-key:}") String apiKey,
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
                return executeGeminiCall(text);
            } catch (Exception e) {
                log.warn("Gemini call attempt {} failed; using the fallback path", attempt);
                if (attempt == 2) {
                    return Optional.empty();
                }
            }
        }

        return Optional.empty();
    }

    private Optional<LlmCategoryResult> executeGeminiCall(String text) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent";

        String prompt = "Categorize this expense sentence: \"" + text.replace("\"", "\\\"") + "\".\n"
                + "Allowed categories: FOOD_DINING, GROCERIES, TRAVEL, SHOPPING, BILLS_UTILITIES, HEALTH, ENTERTAINMENT, SAVINGS, or null if unknown.\n"
                + "Respond strictly in JSON: {\"category\": \"CATEGORY_NAME or null\", \"merchant\": \"merchant name or null\"}";

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json"
                )
        );

        String responseBody = restClient.post()
                .uri(url)
                .header("x-goog-api-key", apiKey)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        if (responseBody == null || responseBody.isBlank()) {
            return Optional.empty();
        }

        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
        if (parts.isArray() && !parts.isEmpty()) {
            String jsonText = parts.get(0).path("text").asText();
            JsonNode resultJson = objectMapper.readTree(jsonText);

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
