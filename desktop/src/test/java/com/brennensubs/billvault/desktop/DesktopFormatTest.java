package com.brennensubs.billvault.desktop;

import static org.junit.jupiter.api.Assertions.*;

import com.brennensubs.billvault.dto.Api.ExpenseView;
import com.brennensubs.billvault.model.BillingFrequency;
import com.brennensubs.billvault.model.ExpenseKind;
import com.brennensubs.billvault.model.ExpenseStatus;
import com.brennensubs.billvault.model.IntervalUnit;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DesktopFormatTest {
  @Test
  void formatsMoneyDatesEnumsAndInitialsCleanly() {
    assertEquals("USD 1,250.50", DesktopFormat.money("USD", new BigDecimal("1250.5")));
    assertEquals("EUR 0.00", DesktopFormat.money("EUR", null));
    assertEquals("Nov 10, 2026", DesktopFormat.dateLabel(LocalDate.of(2026, 11, 10)));
    assertEquals("Subscription", DesktopFormat.titleCase(ExpenseKind.SUBSCRIPTION));
    assertEquals("HF", DesktopFormat.initials("Home fiber"));
    assertEquals("S", DesktopFormat.initials("Streambox"));
    assertEquals("#6056dc", DesktopFormat.safeHexColor("invalid", "#6056dc"));
    assertEquals("#10b981", DesktopFormat.safeHexColor("#10b981", "#6056dc"));

    ExpenseView custom =
        new ExpenseView(
            1L,
            "Domain",
            "",
            ExpenseKind.SUBSCRIPTION,
            1L,
            "Software",
            "#6366f1",
            new BigDecimal("18.00"),
            "USD",
            BillingFrequency.CUSTOM,
            2,
            IntervalUnit.YEARS,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2028, 1, 1),
            null,
            "",
            "",
            ExpenseStatus.ACTIVE,
            false,
            "",
            0L,
            Instant.now(),
            Instant.now());
    assertEquals("Every 2 years", DesktopFormat.frequencyLabel(custom));
  }
}
