package com.brennensubs.billvault;

import static org.junit.jupiter.api.Assertions.*;

import com.brennensubs.billvault.dto.Api.*;
import com.brennensubs.billvault.dto.MobileApi.*;
import com.brennensubs.billvault.model.*;
import com.brennensubs.billvault.service.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MobileSyncTests {
  @Autowired MobileSyncService mobile;
  @Autowired SubscriptionService service;
  @Autowired ObjectMapper json;

  ExpenseRequest fixture(String name) {
    return new ExpenseRequest(
        name,
        null,
        ExpenseKind.BILL,
        service.categories().getFirst().id(),
        new BigDecimal("15.00"),
        "USD",
        BillingFrequency.MONTHLY,
        null,
        null,
        LocalDate.of(2026, 1, 31),
        LocalDate.of(2026, 1, 31),
        null,
        null,
        null,
        ExpenseStatus.ACTIVE,
        false,
        null,
        null);
  }

  @Test
  void retriesDoNotDuplicateExpensesOrAdvancePaymentsTwice() {
    UUID id = UUID.randomUUID();
    var create =
        new Command(id, "CREATE", null, json.valueToTree(fixture("Mobile fictional " + id)));
    var first = mobile.command(create);
    assertEquals(first, mobile.command(create));
    assertThrows(
        ResponseStatusException.class,
        () ->
            mobile.command(
                new Command(id, "CREATE", null, json.valueToTree(fixture("Different content")))));
    var payment =
        new Command(
            UUID.randomUUID(),
            "PAY",
            first.expense().id(),
            json.valueToTree(
                new PaymentRequest(
                    first.expense().nextDueDate(),
                    LocalDate.now(),
                    new BigDecimal("16.50"),
                    "Fictional payment")));
    var paid = mobile.command(payment);
    assertEquals(paid, mobile.command(payment));
    assertEquals(LocalDate.of(2026, 2, 28), service.get(first.expense().id()).nextDueDate());
    assertEquals(
        1,
        service.payments().stream()
            .filter(p -> p.expenseId().equals(first.expense().id()))
            .count());
    assertTrue(
        mobile.snapshot().expenses().stream().anyMatch(e -> e.id().equals(first.expense().id())));
    service.delete(first.expense().id());
  }

  @Test
  void invalidCommandsRollBackAndSnapshotsAreVersioned() {
    assertEquals(1, mobile.snapshot().schemaVersion());
    assertThrows(
        ResponseStatusException.class,
        () ->
            mobile.command(new Command(UUID.randomUUID(), "DELETE", 1L, json.createObjectNode())));
    var invalid = json.valueToTree(fixture(" "));
    assertThrows(
        ResponseStatusException.class,
        () -> mobile.command(new Command(UUID.randomUUID(), "CREATE", null, invalid)));
  }
}
