package com.brennensubs.billvault.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "expenses")
public class Expense {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false, length = 120)
  public String name;

  @Column(length = 1000)
  public String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  public ExpenseKind kind;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "category_id", nullable = false)
  public Category category;

  @Column(nullable = false, precision = 12, scale = 2)
  public BigDecimal amount;

  @Column(nullable = false, length = 3)
  public String currency;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  public BillingFrequency frequency;

  public Integer customInterval;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  public IntervalUnit customUnit;

  @Column(nullable = false)
  public LocalDate startDate;

  @Column(nullable = false)
  public LocalDate nextDueDate;

  public LocalDate endDate;

  @Column(length = 120)
  public String paymentMethod;

  @Column(length = 500)
  public String websiteUrl;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  public ExpenseStatus status;

  @Column(nullable = false)
  public boolean variableAmount;

  @Column(length = 2000)
  public String notes;

  @Version public long version;

  @Column(nullable = false, updatable = false)
  public Instant createdAt;

  @Column(nullable = false)
  public Instant updatedAt;

  @PrePersist
  void created() {
    createdAt = Instant.now();
    updatedAt = createdAt;
  }

  @PreUpdate
  void updated() {
    updatedAt = Instant.now();
  }
}
