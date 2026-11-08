package com.brennensubs.billvault.repository;

import com.brennensubs.billvault.model.ReminderRule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderRuleRepository extends JpaRepository<ReminderRule, Long> {
  java.util.Optional<ReminderRule> findByExpenseId(Long expenseId);
}
