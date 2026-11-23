package com.kharcha.agent.scheduler;

import com.kharcha.agent.service.AgentService;
import com.kharcha.auth.domain.User;
import com.kharcha.auth.repository.UserRepository;
import com.kharcha.common.util.MonthUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Runs agent proposals daily at 09:00 Asia/Kolkata for every registered user.
 * Uses the same service method as the HTTP endpoint — no duplicated calculation.
 */
@Component
public class AgentScheduler {

    private static final Logger log = LoggerFactory.getLogger(AgentScheduler.class);

    private final AgentService agentService;
    private final UserRepository userRepository;
    private final Clock clock;

    public AgentScheduler(AgentService agentService, UserRepository userRepository, Clock clock) {
        this.agentService = agentService;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    /**
     * Fires every day at 09:00 Asia/Kolkata.
     * Loads the current month and runs proposals for all users.
     * One user failing does not block others.
     */
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Kolkata")
    public void runDailyProposals() {
        String month = MonthUtils.formatMonth(YearMonth.from(LocalDate.now(clock)));
        List<User> users = userRepository.findAll();
        log.info("Agent daily run: month={} users={}", month, users.size());

        for (User user : users) {
            try {
                agentService.runProposalsFor(user.getId(), month);
            } catch (Exception ex) {
                log.warn("Agent daily run failed for userId={}: {}", user.getId(), ex.getMessage());
            }
        }
    }
}
