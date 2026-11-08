package com.brennensubs.billvault.config;

import com.brennensubs.billvault.service.SubscriptionService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;

@Configuration
@EnableScheduling
public class ReminderScheduler {
  private final SubscriptionService service;

  public ReminderScheduler(SubscriptionService service) {
    this.service = service;
  }

  @Scheduled(
      fixedDelayString = "${billvault.reminders.interval-ms:3600000}",
      initialDelayString = "${billvault.reminders.initial-delay-ms:60000}")
  public void refresh() {
    service.refreshNotifications();
  }
}
