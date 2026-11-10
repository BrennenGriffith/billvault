package com.brennensubs.billvault;

import static org.junit.jupiter.api.Assertions.*;

import com.brennensubs.billvault.dto.Api.*;
import com.brennensubs.billvault.dto.ImportApi.*;
import com.brennensubs.billvault.model.*;
import com.brennensubs.billvault.service.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ExpenseImportTests {
  @Autowired ExpenseImportService imports;
  @Autowired SubscriptionService service;
  @Autowired JdbcTemplate jdbc;

  ExpenseRequest expense(String name) {
    return new ExpenseRequest(
        name,
        null,
        ExpenseKind.SUBSCRIPTION,
        service.categories().getFirst().id(),
        new BigDecimal("14.99"),
        "USD",
        BillingFrequency.MONTHLY,
        null,
        null,
        LocalDate.of(2026, 11, 1),
        LocalDate.of(2026, 12, 1),
        null,
        null,
        null,
        ExpenseStatus.ACTIVE,
        false,
        null,
        null);
  }

  long count(String name) {
    return service.list(name, null, null, null, "name", 0, 100).totalElements();
  }

  @Test
  void previewIsReadOnlyAndCommitSkipsDuplicatesAndSurvivesLostResponse() {
    String name = "Import " + UUID.randomUUID();
    var preview =
        imports.preview(
            new Batch(
                "fictional example",
                List.of(expense(name), expense(name.toUpperCase(Locale.ROOT)))));
    assertEquals(0, count(name));
    assertEquals("CREATE", preview.rows().get(0).action());
    assertEquals("SKIP", preview.rows().get(1).action());
    assertThrows(ResponseStatusException.class, () -> imports.commit(preview.previewId(), false));
    var result = imports.commit(preview.previewId(), true);
    assertEquals(1, result.createdIds().size());
    assertEquals(1, result.skipped());
    assertEquals(result, imports.commit(preview.previewId(), true));
    assertEquals(1, count(name));
    assertTrue(
        jdbc.queryForObject(
                "SELECT COUNT(*) FROM expense_import_receipts WHERE preview_id=?",
                Integer.class,
                preview.previewId())
            == 1);
    var again = imports.preview(new Batch("repeat", List.of(expense(name))));
    assertEquals("SKIP", again.rows().getFirst().action());
    service.delete(result.createdIds().getFirst());
  }

  @Test
  void rejectsInvalidBatchBeforeAnyWritesAndRejectsStalePreview() {
    String name = "Stale " + UUID.randomUUID();
    var bad = expense(" ");
    assertThrows(
        ResponseStatusException.class,
        () -> imports.preview(new Batch("test", List.of(expense(name), bad))));
    assertEquals(0, count(name));
    var preview = imports.preview(new Batch("test", List.of(expense(name))));
    var concurrent = service.save(null, expense("Concurrent " + UUID.randomUUID()));
    assertEquals(
        409,
        assertThrows(ResponseStatusException.class, () -> imports.commit(preview.previewId(), true))
            .getStatusCode()
            .value());
    assertEquals(0, count(name));
    service.delete(concurrent.id());
    assertThrows(
        ResponseStatusException.class, () -> imports.commit(UUID.randomUUID().toString(), true));
  }

  @Test
  void failedCommitRollsBackEarlierRowsAndReceipt() {
    String name = "Rollback " + UUID.randomUUID();
    var category =
        service.category(new CategoryRequest("Import temp " + UUID.randomUUID(), "#888888"));
    var r = expense(name + " second");
    var second =
        new ExpenseRequest(
            r.name(),
            null,
            r.kind(),
            category.id(),
            r.amount(),
            r.currency(),
            r.frequency(),
            null,
            null,
            r.startDate(),
            r.nextDueDate(),
            null,
            null,
            null,
            r.status(),
            false,
            null,
            null);
    var preview = imports.preview(new Batch("rollback", List.of(expense(name), second)));
    jdbc.update("DELETE FROM categories WHERE id=?", category.id());
    assertThrows(ResponseStatusException.class, () -> imports.commit(preview.previewId(), true));
    assertEquals(0, count(name));
    assertEquals(
        0,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM expense_import_receipts WHERE preview_id=?",
            Integer.class,
            preview.previewId()));
  }

  @Test
  void semanticValidationRunsDuringPreview() {
    var r = expense("Invalid dates");
    var bad =
        new ExpenseRequest(
            r.name(),
            null,
            r.kind(),
            r.categoryId(),
            r.amount(),
            r.currency(),
            r.frequency(),
            null,
            null,
            LocalDate.of(2026, 12, 1),
            LocalDate.of(2026, 11, 1),
            null,
            null,
            null,
            r.status(),
            false,
            null,
            null);
    assertThrows(
        ResponseStatusException.class, () -> imports.preview(new Batch("test", List.of(bad))));
    assertThrows(
        ResponseStatusException.class,
        () -> imports.preview(new Batch("test", Collections.nCopies(51, r))));
  }
}
