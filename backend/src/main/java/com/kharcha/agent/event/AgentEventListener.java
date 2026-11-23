package com.kharcha.agent.event;

import com.kharcha.agent.service.AgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listens for CategoryThresholdEvent and triggers agent proposal generation asynchronously
 * so the originating HTTP thread is not blocked.
 */
@Component
public class AgentEventListener {

    private static final Logger log = LoggerFactory.getLogger(AgentEventListener.class);

    private final AgentService agentService;

    public AgentEventListener(AgentService agentService) {
        this.agentService = agentService;
    }

    @Async
    @EventListener
    public void onCategoryThreshold(CategoryThresholdEvent event) {
        log.info("CategoryThresholdEvent received: userId={} category={} month={}",
                event.userId(), event.category(), event.month());
        try {
            agentService.runProposalsFor(event.userId(), event.month());
        } catch (Exception ex) {
            log.warn("Agent run triggered by threshold event failed for userId={}: {}",
                    event.userId(), ex.getMessage());
        }
    }
}
