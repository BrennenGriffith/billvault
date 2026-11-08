package com.brennensubs.billvault.model;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "reminder_rules")
public class ReminderRule {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "expense_id", nullable = false, unique = true)
  public Expense expense;

  @Column(nullable = false)
  public int daysBefore = 3;

  @Column(nullable = false)
  public boolean enabled = true;
}
