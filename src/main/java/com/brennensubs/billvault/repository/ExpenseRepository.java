package com.brennensubs.billvault.repository;

import com.brennensubs.billvault.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository
    extends JpaRepository<Expense, Long>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<Expense> {
  java.util.List<Expense> findAllByOrderByNextDueDateAsc();

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select e from Expense e where e.id = :id")
  java.util.Optional<Expense> findForPayment(
      @org.springframework.data.repository.query.Param("id") Long id);
}
