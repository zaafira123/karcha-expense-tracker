package com.kharcha.settings.service;

import com.kharcha.auth.domain.User;
import com.kharcha.auth.repository.UserRepository;
import com.kharcha.common.error.ResourceNotFoundException;
import com.kharcha.settings.dto.AgentCapUpdateResponse;
import com.kharcha.settings.dto.SettingsResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SettingsService {
    private final UserRepository userRepository;

    public SettingsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public SettingsResponse getSettings(UUID userId) {
        User user = findUser(userId);
        return new SettingsResponse(user.getEmail(), user.getAgentCapPaise());
    }

    @Transactional
    public AgentCapUpdateResponse updateAgentCap(UUID userId, long amountPaise) {
        User user = findUser(userId);
        user.setAgentCapPaise(amountPaise);
        return new AgentCapUpdateResponse(userRepository.save(user).getAgentCapPaise());
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User settings were not found"));
    }
}
