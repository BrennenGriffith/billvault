package com.brennensubs.billvault;

import static org.junit.jupiter.api.Assertions.*;

import com.brennensubs.billvault.model.*;
import com.brennensubs.billvault.service.BillingCalculator;
import java.math.*;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class BillingCalculatorTests {
  final BillingCalculator billing = new BillingCalculator();

  Expense expense(BillingFrequency f, String anchor) {
    Expense e = new Expense();
    e.frequency = f;
    e.startDate = LocalDate.parse(anchor);
    e.amount = new BigDecimal("120.00");
    return e;
  }

  @Test
  void january31ReturnsTo31AfterFebruary() {
    Expense e = expense(BillingFrequency.MONTHLY, "2024-01-31");
    LocalDate feb = billing.next(e, e.startDate);
    assertEquals(LocalDate.of(2024, 2, 29), feb);
    assertEquals(LocalDate.of(2024, 3, 31), billing.next(e, feb));
  }

  @Test
  void nonLeapFebruaryAndThirtyDayMonth() {
    Expense e = expense(BillingFrequency.MONTHLY, "2025-01-31");
    assertEquals(LocalDate.of(2025, 2, 28), billing.next(e, e.startDate));
    assertEquals(LocalDate.of(2025, 4, 30), billing.next(e, LocalDate.of(2025, 3, 31)));
  }

  @Test
  void yearlyLeapDayRecoversInLeapYear() {
    Expense e = expense(BillingFrequency.YEARLY, "2024-02-29");
    assertEquals(LocalDate.of(2025, 2, 28), billing.next(e, e.startDate));
    assertEquals(LocalDate.of(2028, 2, 29), billing.next(e, LocalDate.of(2027, 2, 28)));
  }

  @Test
  void quarterlyAndWeekly() {
    Expense e = expense(BillingFrequency.QUARTERLY, "2026-01-31");
    assertEquals(LocalDate.of(2026, 4, 30), billing.next(e, e.startDate));
    e.frequency = BillingFrequency.WEEKLY;
    assertEquals(LocalDate.of(2026, 2, 7), billing.next(e, e.startDate));
  }

  @Test
  void customIntervalsAndNormalizedCost() {
    Expense e = expense(BillingFrequency.CUSTOM, "2026-01-31");
    e.customInterval = 2;
    e.customUnit = IntervalUnit.MONTHS;
    assertEquals(LocalDate.of(2026, 3, 31), billing.next(e, e.startDate));
    assertEquals(new BigDecimal("60.00"), billing.monthly(e).setScale(2, RoundingMode.HALF_UP));
    e.frequency = BillingFrequency.YEARLY;
    assertEquals(new BigDecimal("10.00"), billing.monthly(e).setScale(2, RoundingMode.HALF_UP));
  }
}
