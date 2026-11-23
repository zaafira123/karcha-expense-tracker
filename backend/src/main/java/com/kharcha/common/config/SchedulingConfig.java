package com.kharcha.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's @Scheduled and @Async support.
 * Scheduling is used by AgentScheduler; async is used by AgentEventListener.
 */
@Configuration
@EnableScheduling
@EnableAsync
public class SchedulingConfig {
}
