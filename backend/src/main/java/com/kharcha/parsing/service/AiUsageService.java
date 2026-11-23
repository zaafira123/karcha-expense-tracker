package com.kharcha.parsing.service;

import com.kharcha.parsing.domain.AiUsage;
import com.kharcha.parsing.repository.AiUsageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class AiUsageService {

    private final AiUsageRepository aiUsageRepository;
    private final Clock clock;
    private final int dailyLimit;

    public AiUsageService(
            AiUsageRepository aiUsageRepository,
            Clock clock,
            @Value("${ai.daily-limit:20}") int dailyLimit
    ) {
        this.aiUsageRepository = aiUsageRepository;
        this.clock = clock;
        this.dailyLimit = dailyLimit;
    }

    @Transactional
    public boolean canCallAiAndIncrement(UUID userId) {
        LocalDate today = LocalDate.now(clock);

        AiUsage usage = aiUsageRepository.findByUserIdAndUsageDate(userId, today)
                .orElseGet(() -> new AiUsage(UUID.randomUUID(), userId, today, 0));

        if (usage.getCalls() >= dailyLimit) {
            return false;
        }

        usage.incrementCalls();
        aiUsageRepository.save(usage);
        return true;
    }

    @Transactional(readOnly = true)
    public int getUsageCount(UUID userId) {
        LocalDate today = LocalDate.now(clock);
        return aiUsageRepository.findByUserIdAndUsageDate(userId, today)
                .map(AiUsage::getCalls)
                .orElse(0);
    }
}
