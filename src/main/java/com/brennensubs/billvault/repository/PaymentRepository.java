package com.brennensubs.billvault.repository;

import com.brennensubs.billvault.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
  boolean existsByExpenseIdAndOccurrenceDate(Long expenseId, java.time.LocalDate occurrenceDate);

  java.util.List<Payment> findAllByOrderByPaymentDateDescIdDesc();
}
