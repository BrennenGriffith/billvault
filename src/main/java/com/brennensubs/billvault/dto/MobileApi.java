package com.brennensubs.billvault.dto;

import com.brennensubs.billvault.dto.Api.*;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import tools.jackson.databind.JsonNode;

public final class MobileApi {
  private MobileApi() {}

  public record Snapshot(
      int schemaVersion,
      Instant generatedAt,
      List<CategoryView> categories,
      List<ExpenseView> expenses,
      List<PaymentView> payments,
      SettingsView settings) {}

  public record Command(
      @NotNull java.util.UUID commandId,
      @NotBlank String operation,
      Long expenseId,
      @NotNull JsonNode payload) {}

  public record Result(String commandId, ExpenseView expense, PaymentView payment) {}
}
