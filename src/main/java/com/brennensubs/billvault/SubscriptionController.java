package com.brennensubs.billvault;

import com.brennensubs.billvault.dto.Api.*;
import com.brennensubs.billvault.model.*;
import com.brennensubs.billvault.service.SubscriptionService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class SubscriptionController {
  private final SubscriptionService service;

  public SubscriptionController(SubscriptionService service) {
    this.service = service;
  }

  @GetMapping({"/expenses", "/subscriptions"})
  public Page<ExpenseView> list(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) ExpenseKind kind,
      @RequestParam(required = false) ExpenseStatus status,
      @RequestParam(required = false) Long categoryId,
      @RequestParam(defaultValue = "nextDueDate") String sort,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return service.list(search, kind, status, categoryId, sort, page, size);
  }

  @GetMapping({"/expenses/{id}", "/subscriptions/{id}"})
  public ExpenseView get(@PathVariable long id) {
    return service.get(id);
  }

  @PostMapping({"/expenses", "/subscriptions"})
  @ResponseStatus(HttpStatus.CREATED)
  public ExpenseView create(@Valid @RequestBody ExpenseRequest request) {
    return service.save(null, request);
  }

  @PutMapping({"/expenses/{id}", "/subscriptions/{id}"})
  public ExpenseView update(@PathVariable long id, @Valid @RequestBody ExpenseRequest request) {
    return service.save(id, request);
  }

  @DeleteMapping({"/expenses/{id}", "/subscriptions/{id}"})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable long id) {
    service.delete(id);
  }

  @GetMapping("/categories")
  public List<CategoryView> categories() {
    return service.categories();
  }

  @PostMapping("/categories")
  @ResponseStatus(HttpStatus.CREATED)
  public CategoryView category(@Valid @RequestBody CategoryRequest request) {
    return service.category(request);
  }

  @GetMapping("/payments")
  public List<PaymentView> payments() {
    return service.payments();
  }

  @PostMapping("/expenses/{id}/payments")
  @ResponseStatus(HttpStatus.CREATED)
  public PaymentView pay(@PathVariable long id, @Valid @RequestBody PaymentRequest request) {
    return service.pay(id, request);
  }

  @GetMapping("/dashboard")
  public Dashboard dashboard() {
    return service.dashboard();
  }

  @GetMapping("/calendar")
  public List<Map<String, Object>> calendar(
      @RequestParam LocalDate from, @RequestParam LocalDate to) {
    return service.calendar(from, to);
  }

  @GetMapping("/expenses/{id}/reminder")
  public ReminderView reminder(@PathVariable long id) {
    return service.reminder(id);
  }

  @PutMapping("/expenses/{id}/reminder")
  public ReminderView reminder(@PathVariable long id, @Valid @RequestBody ReminderRequest request) {
    return service.reminder(id, request);
  }

  @GetMapping("/notifications")
  public List<NotificationView> notifications() {
    return service.notifications();
  }

  @PostMapping("/notifications/refresh")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void refresh() {
    service.refreshNotifications();
  }

  @PatchMapping("/notifications/{id}/read")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void read(@PathVariable long id) {
    service.readNotification(id);
  }

  @GetMapping("/settings")
  public SettingsView settings() {
    return service.settings();
  }

  @PutMapping("/settings")
  public SettingsView settings(@Valid @RequestBody SettingsRequest request) {
    return service.settings(request);
  }

  @GetMapping("/reports/summary")
  public List<ReportTotal> report(@RequestParam int year, @RequestParam int month) {
    return service.report(year, month);
  }

  @GetMapping(value = "/reports/payments.csv", produces = "text/csv")
  public ResponseEntity<String> csv(@RequestParam LocalDate from, @RequestParam LocalDate to) {
    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=billvault-payments.csv")
        .body(service.csv(from, to));
  }
}
