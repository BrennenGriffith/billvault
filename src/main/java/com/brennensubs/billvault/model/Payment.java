package com.brennensubs.billvault.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "payments")
public class Payment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "expense_id", nullable = false)
  public Expense expense;

  @Column(nullable = false)
  public LocalDate occurrenceDate;

  @Column(nullable = false)
  public LocalDate paymentDate;

  @Column(nullable = false, precision = 12, scale = 2)
  public BigDecimal amount;

  @Column(nullable = false, length = 3)
  public String currency;

  @Column(length = 2000)
  public String notes;

  @Column(nullable = false)
  public Instant createdAt = Instant.now();
}
