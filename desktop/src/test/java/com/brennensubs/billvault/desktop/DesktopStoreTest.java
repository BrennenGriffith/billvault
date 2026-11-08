package com.brennensubs.billvault.desktop;

import static org.junit.jupiter.api.Assertions.*;

import com.brennensubs.billvault.dto.Api.*;
import com.brennensubs.billvault.model.*;
import jakarta.validation.ConstraintViolationException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DesktopStoreTest {
  @TempDir Path temporary;

  @Test
  void persistsPaymentsAcrossRestartWithoutWebServerOrCredentials() throws Exception {
    long id;
    try (var local = new DesktopStore(temporary)) {
      assertFalse(local.isWebServerRunning());
      var category = local.service.categories().getFirst();
      var request =
          new ExpenseRequest(
              "Desktop fixture",
              "",
              ExpenseKind.SUBSCRIPTION,
              category.id(),
              new BigDecimal("12.50"),
              "USD",
              BillingFrequency.MONTHLY,
              null,
              null,
              LocalDate.of(2026, 1, 31),
              LocalDate.of(2026, 1, 31),
              null,
              "",
              "",
              ExpenseStatus.ACTIVE,
              false,
              "",
              null);
      var saved = local.service.save(null, local.validated(request));
      id = saved.id();
      var payment =
          new PaymentRequest(
              saved.nextDueDate(), LocalDate.of(2026, 1, 31), new BigDecimal("12.50"), "fixture");
      local.service.pay(id, local.validated(payment));
      assertEquals(LocalDate.of(2026, 2, 28), local.service.get(id).nextDueDate());
      assertThrows(
          org.springframework.web.server.ResponseStatusException.class,
          () -> local.service.pay(id, payment));
      assertThrows(
          ConstraintViolationException.class,
          () ->
              local.validated(
                  new PaymentRequest(LocalDate.now(), LocalDate.now(), BigDecimal.ZERO, "")));
    }
    try (var reopened = new DesktopStore(temporary)) {
      assertEquals("Desktop fixture", reopened.service.get(id).name());
      assertEquals(1, reopened.service.payments().size());
      assertEquals(
          new BigDecimal("12.50"), reopened.service.report(2026, 1).getFirst().paidInMonth());
      assertTrue(
          reopened
              .service
              .csv(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
              .contains("Desktop fixture"));
    }
  }

  @Test
  void rejectsAnAlreadyOpenVaultInsteadOfStartingASecondWriter() throws Exception {
    try (var first = new DesktopStore(temporary)) {
      assertThrows(
          Exception.class,
          () -> {
            try (var second = new DesktopStore(temporary)) {
              fail("Second writer opened");
            }
          });
      assertNotNull(first.service.categories());
    }
  }
}
