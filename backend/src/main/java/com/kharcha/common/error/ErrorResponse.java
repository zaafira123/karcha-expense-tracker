package com.kharcha.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String error,
        String message,
        Map<String, String> fields
) {
    public ErrorResponse(String error, String message) {
        this(error, message, Map.of());
    }
}
