package com.brennensubs.billvault.model;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "notifications")
public class Notification {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "expense_id", nullable = false)
  public Expense expense;

  @Column(nullable = false)
  public LocalDate occurrenceDate;

  @Column(nullable = false, length = 500)
  public String message;

  @Column(nullable = false)
  public boolean read = false;

  @Column(nullable = false)
  public Instant createdAt = Instant.now();
}
