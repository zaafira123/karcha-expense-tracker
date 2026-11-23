package com.kharcha.parsing.service;

import com.kharcha.common.domain.Category;
import com.kharcha.parsing.domain.UserMerchantCategory;
import com.kharcha.parsing.repository.UserMerchantCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class MerchantCorrectionService {

    private final UserMerchantCategoryRepository repository;

    public MerchantCorrectionService(UserMerchantCategoryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<Category> findCorrection(UUID userId, String merchant) {
        if (userId == null || merchant == null || merchant.isBlank()) {
            return Optional.empty();
        }

        String key = merchant.trim().toLowerCase(Locale.ROOT);
        return repository.findByUserIdAndMerchantKey(userId, key)
                .map(UserMerchantCategory::getCategory);
    }

    @Transactional(readOnly = true)
    public Optional<UserMerchantCategory> findMatchingCorrection(UUID userId, String text) {
        if (userId == null || text == null || text.isBlank()) {
            return Optional.empty();
        }

        List<UserMerchantCategory> list = repository.findByUserId(userId);
        if (list.isEmpty()) {
            return Optional.empty();
        }

        // Check longer merchant keys first
        List<UserMerchantCategory> sorted = list.stream()
                .sorted(Comparator.comparingInt((UserMerchantCategory c) -> c.getMerchantKey().length()).reversed())
                .toList();

        for (UserMerchantCategory umc : sorted) {
            Pattern p = Pattern.compile("(?i)\\b" + Pattern.quote(umc.getMerchantKey()) + "\\b");
            if (p.matcher(text).find()) {
                return Optional.of(umc);
            }
        }

        return Optional.empty();
    }

    @Transactional
    public void recordCorrection(UUID userId, String merchant, Category category) {
        if (userId == null || merchant == null || merchant.isBlank() || category == null) {
            return;
        }

        String key = merchant.trim().toLowerCase(Locale.ROOT);
        UserMerchantCategory entity = repository.findByUserIdAndMerchantKey(userId, key)
                .orElseGet(() -> new UserMerchantCategory(UUID.randomUUID(), userId, key, category, Instant.now()));

        entity.setCategory(category);
        entity.setUpdatedAt(Instant.now());
        repository.save(entity);
    }
}
