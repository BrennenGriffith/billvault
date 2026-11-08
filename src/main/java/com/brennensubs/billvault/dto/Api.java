package com.brennensubs.billvault.dto;

import com.brennensubs.billvault.model.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class Api {
  private Api() {}

  public record ExpenseRequest(
      @NotBlank @Size(max = 120) String name,
      @Size(max = 1000) String description,
      @NotNull ExpenseKind kind,
      @NotNull Long categoryId,
      @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
      @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
      @NotNull BillingFrequency frequency,
      @Min(1) @Max(3650) Integer customInterval,
      IntervalUnit customUnit,
      @NotNull LocalDate startDate,
      @NotNull LocalDate nextDueDate,
      LocalDate endDate,
      @Size(max = 120) String paymentMethod,
      @Size(max = 500)
          @Pattern(regexp = "^$|https?://[^\\s]+", message = "must be an http or https URL")
          String websiteUrl,
      @NotNull ExpenseStatus status,
      boolean variableAmount,
      @Size(max = 2000) String notes,
      @PositiveOrZero Long version) {}

  public record ExpenseView(
      Long id,
      String name,
      String description,
      ExpenseKind kind,
      Long categoryId,
      String categoryName,
      String categoryColor,
      BigDecimal amount,
      String currency,
      BillingFrequency frequency,
      Integer customInterval,
      IntervalUnit customUnit,
      LocalDate startDate,
      LocalDate nextDueDate,
      LocalDate endDate,
      String paymentMethod,
      String websiteUrl,
      ExpenseStatus status,
      boolean variableAmount,
      String notes,
      long version,
      Instant createdAt,
      Instant updatedAt) {}

  public record CategoryRequest(
      @NotBlank @Size(max = 80) String name,
      @NotBlank @Pattern(regexp = "#[0-9a-fA-F]{6}") String color) {}

  public record CategoryView(Long id, String name, String color) {}

  public record PaymentRequest(
      @NotNull LocalDate occurrenceDate,
      @NotNull @PastOrPresent LocalDate paymentDate,
      @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
      @Size(max = 2000) String notes) {}

  public record PaymentView(
      Long id,
      Long expenseId,
      String expenseName,
      LocalDate occurrenceDate,
      LocalDate paymentDate,
      BigDecimal amount,
      String currency,
      String notes) {}

  public record ReminderRequest(@Min(0) @Max(90) int daysBefore, boolean enabled) {}

  public record ReminderView(Long expenseId, int daysBefore, boolean enabled) {}

  public record NotificationView(
      Long id,
      Long expenseId,
      String message,
      LocalDate occurrenceDate,
      boolean read,
      Instant createdAt) {}

  public record SettingsRequest(
      @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
      @NotBlank @Pattern(regexp = "light|dark") String theme,
      @Min(0) @Max(90) int reminderDays) {}

  public record SettingsView(String currency, String theme, int reminderDays) {}

  public record Page<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}

  public record ReportTotal(String currency, BigDecimal paidInMonth, BigDecimal paidInYear) {}

  public record MoneyTotal(
      String currency, BigDecimal monthly, BigDecimal annual, BigDecimal paidThisMonth) {}

  public record CategoryTotal(String category, String currency, BigDecimal monthly) {}

  public record Trend(String month, String currency, BigDecimal amount) {}

  public record Dashboard(
      long activeCount,
      List<MoneyTotal> totals,
      List<ExpenseView> upcoming7,
      List<ExpenseView> upcoming30,
      List<ExpenseView> overdue,
      List<CategoryTotal> categories,
      List<ExpenseView> mostExpensive,
      List<PaymentView> recentPayments,
      List<Trend> trends) {}
}
