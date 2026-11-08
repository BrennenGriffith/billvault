package com.brennensubs.billvault.service;

import com.brennensubs.billvault.model.*;
import java.math.*;
import java.time.*;
import org.springframework.stereotype.Component;

@Component
public class BillingCalculator {
  public LocalDate next(Expense e, LocalDate current) {
    return switch (e.frequency) {
      case WEEKLY -> current.plusWeeks(1);
      case MONTHLY -> monthDate(current.plusMonths(1), e.startDate.getDayOfMonth());
      case QUARTERLY -> monthDate(current.plusMonths(3), e.startDate.getDayOfMonth());
      case YEARLY -> yearDate(current.plusYears(1), e.startDate);
      case CUSTOM ->
          switch (e.customUnit) {
            case DAYS -> current.plusDays(e.customInterval);
            case WEEKS -> current.plusWeeks(e.customInterval);
            case MONTHS ->
                monthDate(current.plusMonths(e.customInterval), e.startDate.getDayOfMonth());
            case YEARS -> yearDate(current.plusYears(e.customInterval), e.startDate);
          };
    };
  }

  // Clamp short months without losing the original start-date day on later occurrences.
  private LocalDate monthDate(LocalDate date, int day) {
    return date.withDayOfMonth(Math.min(day, date.lengthOfMonth()));
  }

  private LocalDate yearDate(LocalDate date, LocalDate anchor) {
    return YearMonth.of(date.getYear(), anchor.getMonth())
        .atDay(
            Math.min(
                anchor.getDayOfMonth(),
                YearMonth.of(date.getYear(), anchor.getMonth()).lengthOfMonth()));
  }

  // A normalized commitment estimate; actual cash flow comes from Payment records.
  public BigDecimal monthly(Expense e) {
    BigDecimal factor =
        switch (e.frequency) {
          case WEEKLY -> divide(52, 12);
          case MONTHLY -> BigDecimal.ONE;
          case QUARTERLY -> divide(1, 3);
          case YEARLY -> divide(1, 12);
          case CUSTOM ->
              switch (e.customUnit) {
                case DAYS ->
                    divide("365.2425", 12)
                        .divide(BigDecimal.valueOf(e.customInterval), 12, RoundingMode.HALF_UP);
                case WEEKS ->
                    divide(52, 12)
                        .divide(BigDecimal.valueOf(e.customInterval), 12, RoundingMode.HALF_UP);
                case MONTHS -> divide(1, e.customInterval);
                case YEARS -> divide(1, 12L * e.customInterval);
              };
        };
    return e.amount.multiply(factor);
  }

  private BigDecimal divide(long numerator, long denominator) {
    return divide(Long.toString(numerator), denominator);
  }

  private BigDecimal divide(String numerator, long denominator) {
    return new BigDecimal(numerator)
        .divide(BigDecimal.valueOf(denominator), 12, RoundingMode.HALF_UP);
  }
}
