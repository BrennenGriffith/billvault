package com.brennensubs.billvault;

import static org.junit.jupiter.api.Assertions.*;

import com.brennensubs.billvault.dto.Api.*;
import com.brennensubs.billvault.model.*;
import com.brennensubs.billvault.service.SubscriptionService;
import java.math.BigDecimal;
import java.net.*;
import java.net.http.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BillvaultApplicationTests {
  @Autowired SubscriptionService service;
  @Autowired ObjectMapper json;
  @LocalServerPort int port;

  ExpenseRequest request(String name, ExpenseStatus status) {
    return new ExpenseRequest(
        name,
        "test expense",
        ExpenseKind.BILL,
        service.categories().getFirst().id(),
        new BigDecimal("79.99"),
        "USD",
        BillingFrequency.MONTHLY,
        null,
        null,
        LocalDate.of(2026, 1, 31),
        LocalDate.of(2026, 1, 31),
        null,
        "Card",
        "https://example.com",
        status,
        true,
        "notes",
        null);
  }

  @Test
  void paymentPersistsAdvancesDateAndRejectsDuplicates() {
    ExpenseView e = service.save(null, request("Test utilities", ExpenseStatus.ACTIVE));
    PaymentRequest p =
        new PaymentRequest(
            e.nextDueDate(), LocalDate.now(), new BigDecimal("83.21"), "Variable bill");
    PaymentView paid = service.pay(e.id(), p);
    assertEquals(new BigDecimal("83.21"), paid.amount());
    assertEquals(LocalDate.of(2026, 2, 28), service.get(e.id()).nextDueDate());
    assertEquals(
        409,
        assertThrows(ResponseStatusException.class, () -> service.pay(e.id(), p))
            .getStatusCode()
            .value());
    assertTrue(service.payments().stream().anyMatch(x -> x.id().equals(paid.id())));
    assertTrue(
        service.dashboard().totals().stream()
            .anyMatch(t ->
                t.currency().equals("USD") && t.paidThisMonth().compareTo(paid.amount()) >= 0));
    assertTrue(service.csv(LocalDate.now(), LocalDate.now()).contains("83.21"));
    assertTrue(
        service.report(LocalDate.now().getYear(), LocalDate.now().getMonthValue()).stream()
            .anyMatch(
                t -> t.currency().equals("USD") && t.paidInMonth().compareTo(paid.amount()) >= 0));
    service.delete(e.id());
    assertEquals(
        404,
        assertThrows(ResponseStatusException.class, () -> service.get(e.id()))
            .getStatusCode()
            .value());
  }

  @Test
  void validationAndPausedExpense() {
    ExpenseView e = service.save(null, request("Paused bill", ExpenseStatus.PAUSED));
    assertEquals(
        400,
        assertThrows(
                ResponseStatusException.class,
                () ->
                    service.pay(
                        e.id(),
                        new PaymentRequest(e.nextDueDate(), LocalDate.now(), BigDecimal.ONE, null)))
            .getStatusCode()
            .value());
    service.delete(e.id());
    assertEquals(
        404,
        assertThrows(ResponseStatusException.class, () -> service.get(999999))
            .getStatusCode()
            .value());
    assertEquals(
        400,
        assertThrows(
                ResponseStatusException.class,
                () -> service.list(null, null, null, null, "invalid", 0, 10))
            .getStatusCode()
            .value());
  }

  @Test
  void remindersDoNotDuplicateAndCalendarRepeats() {
    ExpenseView e = service.save(null, request("Reminder test", ExpenseStatus.ACTIVE));
    service.refreshNotifications();
    service.refreshNotifications();
    assertEquals(
        1, service.notifications().stream().filter(n -> n.expenseId().equals(e.id())).count());
    var occurrences =
        service.calendar(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31)).stream()
            .filter(x -> x.get("expenseId").equals(e.id()))
            .toList();
    assertEquals(3, occurrences.size());
    assertEquals(LocalDate.of(2026, 3, 31), occurrences.getLast().get("date"));
    service.delete(e.id());
  }

  @Test
  void finalOccurrenceCancelsAndSqlFilteringWorks() {
    ExpenseRequest r = request("Final occurrence SQL test", ExpenseStatus.ACTIVE);
    ExpenseRequest finite =
        new ExpenseRequest(
            r.name(),
            r.description(),
            r.kind(),
            r.categoryId(),
            r.amount(),
            r.currency(),
            r.frequency(),
            null,
            null,
            r.startDate(),
            r.nextDueDate(),
            r.nextDueDate(),
            r.paymentMethod(),
            r.websiteUrl(),
            r.status(),
            r.variableAmount(),
            r.notes(),
            null);
    ExpenseView e = service.save(null, finite);
    assertTrue(service.dashboard().overdue().stream().anyMatch(x -> x.id().equals(e.id())));
    var filtered =
        service.list(
            "Final occurrence SQL",
            ExpenseKind.BILL,
            ExpenseStatus.ACTIVE,
            e.categoryId(),
            "name",
            0,
            1);
    assertEquals(1, filtered.totalElements());
    assertEquals(e.id(), filtered.content().getFirst().id());
    service.pay(
        e.id(),
        new PaymentRequest(e.nextDueDate(), LocalDate.now(), e.amount(), "=formula\nsecond line"));
    assertEquals(ExpenseStatus.CANCELED, service.get(e.id()).status());
    assertTrue(service.csv(LocalDate.now(), LocalDate.now()).contains("'=formula"));
    service.delete(e.id());
  }

  @Test
  void staleExpenseUpdatesAreRejected() {
    ExpenseRequest original = request("Concurrent edit test", ExpenseStatus.ACTIVE);
    ExpenseView e = service.save(null, original);
    ExpenseRequest edit =
        new ExpenseRequest(
            "Updated once",
            original.description(),
            original.kind(),
            original.categoryId(),
            original.amount(),
            original.currency(),
            original.frequency(),
            null,
            null,
            original.startDate(),
            original.nextDueDate(),
            null,
            original.paymentMethod(),
            original.websiteUrl(),
            original.status(),
            original.variableAmount(),
            original.notes(),
            e.version());
    service.save(e.id(), edit);
    assertEquals(
        409,
        assertThrows(ResponseStatusException.class, () -> service.save(e.id(), edit))
            .getStatusCode()
            .value());
    assertEquals("Updated once", service.get(e.id()).name());
    service.delete(e.id());
  }

  @Test
  void httpLoginCsrfValidationAndLogout() throws Exception {
    HttpClient client =
        HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .build();
    var unauthorized = send(client, "/expenses", "GET", null, null);
    assertEquals(401, unauthorized.statusCode());
    assertTrue(
        unauthorized
            .headers()
            .firstValue("Content-Type")
            .orElse("")
            .contains("application/problem+json"));
    assertEquals(
        "Authentication required", json.readTree(unauthorized.body()).get("detail").asText());
    assertTrue(unauthorized.headers().firstValue("Content-Security-Policy").isPresent());
    var token = json.readTree(send(client, "/auth/csrf", "GET", null, null).body());
    String header = token.get("headerName").asText(), csrf = token.get("token").asText();
    HttpRequest login =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/login"))
            .header(header, csrf)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    "username=admin&password=test-password-only-123"))
            .build();
    assertEquals(204, client.send(login, HttpResponse.BodyHandlers.ofString()).statusCode());
    assertEquals(200, send(client, "/expenses", "GET", null, null).statusCode());
    assertEquals(403, send(client, "/expenses", "POST", "{}", null).statusCode());
    token = json.readTree(send(client, "/auth/csrf", "GET", null, null).body());
    csrf = token.get("token").asText();
    assertEquals(400, send(client, "/expenses", "POST", "{}", csrf).statusCode());
    assertEquals(404, send(client, "/expenses/999999", "GET", null, null).statusCode());
    assertEquals(204, send(client, "/auth/logout", "POST", null, csrf).statusCode());
    assertEquals(401, send(client, "/payments", "GET", null, null).statusCode());
  }

  HttpResponse<String> send(HttpClient client, String path, String method, String body, String csrf)
      throws Exception {
    var b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api" + path));
    if (csrf != null) b.header("X-CSRF-TOKEN", csrf);
    if (body != null) b.header("Content-Type", "application/json");
    return client.send(
        b.method(
                method,
                body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }
}
