package com.brennensubs.billvault.dto;

import com.brennensubs.billvault.dto.Api.ExpenseRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public final class ImportApi {
  private ImportApi() {}

  public record Batch(
      @NotBlank @Size(max = 120) String source,
      @NotEmpty @Size(max = 50) List<@NotNull @Valid ExpenseRequest> expenses) {}

  public record Row(int index, ExpenseRequest expense, String action, String reason) {}

  public record Preview(String previewId, Instant expiresAt, String source, List<Row> rows) {}

  public record Confirmation(@NotNull Boolean confirmed) {}

  public record Result(String previewId, List<Long> createdIds, int skipped) {}
}
