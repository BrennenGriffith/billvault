package com.brennensubs.billvault.model;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "app_settings")
public class AppSettings {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false, length = 3)
  public String currency = "USD";

  @Column(nullable = false, length = 10)
  public String theme = "light";

  @Column(nullable = false)
  public int reminderDays = 3;
}
