package com.brennensubs.billvault;

import com.brennensubs.billvault.dto.ImportApi.*;
import com.brennensubs.billvault.service.ExpenseImportService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/imports")
public class ExpenseImportController {
  private final ExpenseImportService imports;

  public ExpenseImportController(ExpenseImportService imports) {
    this.imports = imports;
  }

  @PostMapping("/preview")
  public Preview preview(@Valid @RequestBody Batch batch) {
    return imports.preview(batch);
  }

  @PostMapping("/{id}/commit")
  public Result commit(@PathVariable String id, @Valid @RequestBody Confirmation confirmation) {
    return imports.commit(id, Boolean.TRUE.equals(confirmation.confirmed()));
  }
}
