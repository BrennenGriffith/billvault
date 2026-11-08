package com.brennensubs.billvault.repository;

import com.brennensubs.billvault.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
  java.util.List<Notification> findAllByOrderByCreatedAtDesc();

  boolean existsByExpenseIdAndOccurrenceDate(Long expenseId, java.time.LocalDate occurrenceDate);
}
