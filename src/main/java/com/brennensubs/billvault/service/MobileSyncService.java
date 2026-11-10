package com.brennensubs.billvault.service;

import com.brennensubs.billvault.dto.Api.*;
import com.brennensubs.billvault.dto.MobileApi.*;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validator;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@Service
@Transactional
public class MobileSyncService {
  private final SubscriptionService service;
  private final JdbcTemplate jdbc;
  private final ObjectMapper json;
  private final Validator validator;
  private final EntityManager entities;

  public MobileSyncService(
      SubscriptionService service,
      JdbcTemplate jdbc,
      ObjectMapper json,
      Validator validator,
      EntityManager entities) {
    this.service = service;
    this.jdbc = jdbc;
    this.json = json;
    this.validator = validator;
    this.entities = entities;
  }

  @Transactional(
      readOnly = true,
      isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
  public Snapshot snapshot() {
    var all = new ArrayList<ExpenseView>();
    int page = 0;
    com.brennensubs.billvault.dto.Api.Page<ExpenseView> batch;
    do {
      batch = service.list(null, null, null, null, "name", page++, 100);
      all.addAll(batch.content());
      if (all.size() > 10000)
        throw new ResponseStatusException(
            HttpStatus.PAYLOAD_TOO_LARGE, "Mobile snapshot exceeds 10000 expenses");
    } while (page < batch.totalPages());
    return new Snapshot(
        1,
        Instant.now(),
        service.categories(),
        List.copyOf(all),
        service.payments(),
        service.settings());
  }

  public Result command(Command command) {
    if (command == null || !validator.validate(command).isEmpty())
      throw bad("Invalid mobile command");
    String id = command.commandId().toString();
    String hash =
        hash(
            json.writer()
                .with(tools.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .writeValueAsString(json.convertValue(command, Object.class)));
    var receipt =
        jdbc.queryForList(
            "SELECT request_hash,result_json FROM mobile_command_receipts WHERE command_id=?", id);
    if (!receipt.isEmpty()) {
      if (!hash.equals(receipt.getFirst().get("request_hash")))
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "Command ID reused with different data");
      return json.readValue((String) receipt.getFirst().get("result_json"), Result.class);
    }
    jdbc.update(
        "INSERT INTO mobile_command_receipts(command_id,request_hash,result_json) VALUES (?,?,?)",
        id,
        hash,
        "{}");
    ExpenseView expense;
    PaymentView payment = null;
    switch (command.operation()) {
      case "CREATE" ->
          expense =
              service.save(null, valid(json.treeToValue(command.payload(), ExpenseRequest.class)));
      case "UPDATE" -> {
        if (command.expenseId() == null || command.expenseId() < 1)
          throw bad("Select a saved expense");
        expense =
            service.save(
                command.expenseId(),
                valid(json.treeToValue(command.payload(), ExpenseRequest.class)));
      }
      case "PAY" -> {
        if (command.expenseId() == null || command.expenseId() < 1)
          throw bad("Select a saved expense");
        payment =
            service.pay(
                command.expenseId(),
                valid(json.treeToValue(command.payload(), PaymentRequest.class)));
        entities.flush();
        expense = service.get(command.expenseId());
      }
      default -> throw bad("Unsupported mobile operation");
    }
    var result = new Result(id, expense, payment);
    jdbc.update(
        "UPDATE mobile_command_receipts SET result_json=? WHERE command_id=?",
        json.writeValueAsString(result),
        id);
    return result;
  }

  private <T> T valid(T request) {
    if (request == null || !validator.validate(request).isEmpty())
      throw bad("Invalid expense or payment");
    return request;
  }

  private ResponseStatusException bad(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }

  public static String hash(String text) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
