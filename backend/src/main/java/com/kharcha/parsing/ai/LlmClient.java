package com.kharcha.parsing.ai;

import java.util.Optional;

public interface LlmClient {

    Optional<LlmCategoryResult> categorize(String text);
}
