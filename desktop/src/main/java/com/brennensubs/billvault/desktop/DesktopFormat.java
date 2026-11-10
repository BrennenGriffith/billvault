package com.brennensubs.billvault.desktop;

import com.brennensubs.billvault.dto.Api.ExpenseView;
import com.brennensubs.billvault.model.BillingFrequency;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Pure formatting helpers for human-readable currency, dates, and enum labels in JavaFX views. */
final class DesktopFormat {
  private static final DateTimeFormatter DATE_FORMAT =
      DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);
  private static final DateTimeFormatter MONTH_FORMAT =
      DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US);
  private static final DateTimeFormatter SHORT_MONTH_FORMAT =
      DateTimeFormatter.ofPattern("MMM", Locale.US);
  private static final DateTimeFormatter WEEKDAY_FORMAT =
      DateTimeFormatter.ofPattern("EEE", Locale.US);
  private static final DateTimeFormatter HEADER_DATE_FORMAT =
      DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.US);

  private DesktopFormat() {}

  static String money(String currency, BigDecimal amount) {
    String code = currency == null || currency.isBlank() ? "USD" : currency.trim();
    BigDecimal safe =
        (amount == null ? BigDecimal.ZERO : amount).setScale(2, RoundingMode.HALF_UP);
    DecimalFormat df = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
    return code + " " + df.format(safe);
  }

  static String dateLabel(LocalDate date) {
    return date == null ? "—" : DATE_FORMAT.format(date);
  }

  static String monthLabel(YearMonth ym) {
    return ym == null ? "" : MONTH_FORMAT.format(ym);
  }

  static String shortMonthLabel(String yearMonth) {
    if (yearMonth == null || yearMonth.length() < 7) return yearMonth == null ? "" : yearMonth;
    try {
      return SHORT_MONTH_FORMAT.format(YearMonth.parse(yearMonth.substring(0, 7)));
    } catch (RuntimeException ex) {
      return yearMonth;
    }
  }

  static String weekdayShort(LocalDate date) {
    return date == null ? "" : WEEKDAY_FORMAT.format(date);
  }

  static String todayEyebrow(LocalDate date) {
    return date == null ? "" : HEADER_DATE_FORMAT.format(date).toUpperCase(Locale.US);
  }

  static String titleCase(Enum<?> value) {
    if (value == null) return "";
    String raw = value.name().toLowerCase(Locale.US).replace('_', ' ');
    return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
  }

  static String frequencyLabel(ExpenseView expense) {
    if (expense == null || expense.frequency() == null) return "";
    if (expense.frequency() == BillingFrequency.CUSTOM
        && expense.customInterval() != null
        && expense.customUnit() != null) {
      String unit = expense.customUnit().name().toLowerCase(Locale.US);
      if (expense.customInterval() == 1 && unit.endsWith("s")) {
        unit = unit.substring(0, unit.length() - 1);
      }
      return "Every " + expense.customInterval() + " " + unit;
    }
    return titleCase(expense.frequency());
  }

  static String initials(String name) {
    if (name == null || name.isBlank()) return "B";
    String[] parts = name.trim().split("\\s+");
    if (parts.length >= 2 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
      return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase(Locale.US);
    }
    return ("" + name.trim().charAt(0)).toUpperCase(Locale.US);
  }

  static String safeHexColor(String color, String fallback) {
    if (color != null && color.matches("#[0-9a-fA-F]{6}")) {
      return color;
    }
    return fallback;
  }
}
