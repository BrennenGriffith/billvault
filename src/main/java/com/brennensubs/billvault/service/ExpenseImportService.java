package com.brennensubs.billvault.service;

import com.brennensubs.billvault.dto.ImportApi.*;
import com.brennensubs.billvault.repository.ExpenseRepository;
import jakarta.validation.Validator;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

/** Immutable, expiring previews; receipts and expense inserts commit in one transaction. */
@Service
public class ExpenseImportService {
  private record Pending(Preview preview, String revision) {}

  private final Map<String, Pending> pending = new LinkedHashMap<>();
  private final SubscriptionService service;
  private final ExpenseRepository expenses;
  private final Validator validator;
  private final JdbcTemplate jdbc;
  private final ObjectMapper json;

  public ExpenseImportService(
      SubscriptionService service,
      ExpenseRepository expenses,
      Validator validator,
      JdbcTemplate jdbc,
      ObjectMapper json) {
    this.service = service;
    this.expenses = expenses;
    this.validator = validator;
    this.jdbc = jdbc;
    this.json = json;
  }

  @Transactional(readOnly = true)
  public synchronized Preview preview(Batch batch) {
    if (batch == null || !validator.validate(batch).isEmpty())
      throw error(
          HttpStatus.BAD_REQUEST,
          "Provide a source and 1–50 valid expenses; no bank account details.");
    pending.values().removeIf(p -> p.preview().expiresAt().isBefore(Instant.now()));
    if (pending.size() >= 100)
      throw error(HttpStatus.TOO_MANY_REQUESTS, "Too many pending previews; try again later.");
    var existing = expenses.findAll();
    var keys = new HashSet<String>();
    existing.forEach(e -> keys.add(key(e.name, e.currency, e.kind.name())));
    var rows = new ArrayList<Row>();
    for (int i = 0; i < batch.expenses().size(); i++) {
      var request = batch.expenses().get(i);
      service.validateExpense(request);
      boolean duplicate = !keys.add(key(request.name(), request.currency(), request.kind().name()));
      rows.add(
          new Row(
              i,
              request,
              duplicate ? "SKIP" : "CREATE",
              duplicate
                  ? "Same normalized name, currency, and kind already exists or repeats in this"
                        + " batch; review manually if distinct."
                  : "New recurring expense; no payment will be recorded."));
    }
    var preview =
        new Preview(
            UUID.randomUUID().toString(),
            Instant.now().plusSeconds(600),
            batch.source(),
            List.copyOf(rows));
    pending.put(preview.previewId(), new Pending(preview, revision()));
    return preview;
  }

  @Transactional
  public synchronized Result commit(String id, boolean confirmed) {
    if (!confirmed)
      throw error(HttpStatus.BAD_REQUEST, "Review the preview and explicitly confirm this import.");
    var receipts =
        jdbc.queryForList(
            "SELECT result_json FROM expense_import_receipts WHERE preview_id = ?",
            String.class,
            id);
    if (!receipts.isEmpty()) return json.readValue(receipts.getFirst(), Result.class);
    var entry = pending.get(id);
    if (entry == null || entry.preview().expiresAt().isBefore(Instant.now()))
      throw error(
          HttpStatus.CONFLICT, "Preview expired or server restarted; prepare a new preview.");
    if (!entry.revision().equals(revision()))
      throw error(
          HttpStatus.CONFLICT, "Expenses changed since preview; prepare and review a new preview.");
    // Reserve the unique receipt before any inserts. Concurrent retries roll back on conflict.
    jdbc.update(
        "INSERT INTO expense_import_receipts(preview_id,result_json) VALUES (?,?)", id, "{}");
    var created = new ArrayList<Long>();
    int skipped = 0;
    for (var row : entry.preview().rows()) {
      if (row.action().equals("SKIP")) {
        skipped++;
        continue;
      }
      created.add(service.save(null, row.expense()).id());
    }
    var result = new Result(id, List.copyOf(created), skipped);
    jdbc.update(
        "UPDATE expense_import_receipts SET result_json = ? WHERE preview_id = ?",
        json.writeValueAsString(result),
        id);
    return result;
  }

  private String revision() {
    return expenses.findAll().stream()
        .sorted(Comparator.comparing(e -> e.id))
        .map(e -> e.id + ":" + e.version)
        .reduce("", (a, b) -> a + ";" + b);
  }

  private static String key(String name, String currency, String kind) {
    return name.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT)
        + "|"
        + currency
        + "|"
        + kind;
  }

  private static ResponseStatusException error(HttpStatus status, String message) {
    return new ResponseStatusException(status, message);
  }
}
