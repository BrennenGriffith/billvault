package com.brennensubs.billvault.model;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "categories")
public class Category {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false, unique = true, length = 80)
  public String name;

  @Column(nullable = false, length = 7)
  public String color = "#5b67dc";
}
