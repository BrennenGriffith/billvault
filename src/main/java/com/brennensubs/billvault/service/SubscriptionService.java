package com.brennensubs.billvault.service;

import com.brennensubs.billvault.dto.Api.*;
import com.brennensubs.billvault.model.*;
import com.brennensubs.billvault.repository.*;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.stream.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class SubscriptionService {
  private final ExpenseRepository expenses;
  private final CategoryRepository categories;
  private final PaymentRepository payments;
  private final ReminderRuleRepository rules;
  private final NotificationRepository notifications;
  private final AppSettingsRepository settings;
  private final BillingCalculator billing;

  public SubscriptionService(
      ExpenseRepository e,
      CategoryRepository c,
      PaymentRepository p,
      ReminderRuleRepository r,
      NotificationRepository n,
      AppSettingsRepository s,
      BillingCalculator b) {
    expenses = e;
    categories = c;
    payments = p;
    rules = r;
    notifications = n;
    settings = s;
    billing = b;
  }

  private ResponseStatusException missing(String type) {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, type + " not found");
  }

  private void require(boolean condition, String message) {
    if (!condition) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }

  private Expense entity(long id) {
    return expenses.findById(id).orElseThrow(() -> missing("Expense"));
  }

  public ExpenseView get(long id) {
    return view(entity(id));
  }

  public Page<ExpenseView> list(
      String search,
      ExpenseKind kind,
      ExpenseStatus status,
      Long categoryId,
      String sort,
      int page,
      int size) {
    require(
        page >= 0 && size >= 1 && size <= 100,
        "Page must be nonnegative and size between 1 and 100");
    require(
        Set.of("name", "amount", "nextDueDate").contains(sort),
        "Sort must be name, amount, or nextDueDate");
    org.springframework.data.jpa.domain.Specification<Expense> filter =
        (root, query, cb) -> {
          List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
          if (search != null && !search.isBlank())
            predicates.add(
                cb.like(
                    cb.lower(root.get("name")),
                    "%"
                        + search
                            .toLowerCase(Locale.ROOT)
                            .replace("\\", "\\\\")
                            .replace("%", "\\%")
                            .replace("_", "\\_")
                        + "%",
                    '\\'));
          if (kind != null) predicates.add(cb.equal(root.get("kind"), kind));
          if (status != null) predicates.add(cb.equal(root.get("status"), status));
          if (categoryId != null)
            predicates.add(cb.equal(root.get("category").get("id"), categoryId));
          return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    var ordering =
        sort.equals("amount")
            ? org.springframework.data.domain.Sort.by("currency", "amount", "id")
            : org.springframework.data.domain.Sort.by(sort, "id");
    var result =
        expenses.findAll(
            filter, org.springframework.data.domain.PageRequest.of(page, size, ordering));
    return new Page<>(
        result.getContent().stream().map(this::view).toList(),
        page,
        size,
        result.getTotalElements(),
        result.getTotalPages());
  }

  public void validateExpense(ExpenseRequest r) {
    require(
        !r.nextDueDate().isBefore(r.startDate()), "Next due date must be on or after start date");
    require(
        r.endDate() == null || !r.endDate().isBefore(r.nextDueDate()),
        "End date must be on or after next due date");
    require(
        r.frequency() != BillingFrequency.CUSTOM
            || (r.customInterval() != null && r.customUnit() != null),
        "Custom billing requires interval and unit");
    try {
      Currency.getInstance(r.currency());
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown currency");
    }
    require(categories.existsById(r.categoryId()), "Category not found");
  }

  public ExpenseView save(Long id, ExpenseRequest r) {
    validateExpense(r);
    Expense e =
        id == null
            ? new Expense()
            : expenses.findForPayment(id).orElseThrow(() -> missing("Expense"));
    if (id != null && (r.version() == null || r.version() != e.version)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "This expense changed since you opened it. Refresh before editing again.");
    }
    e.name = r.name().trim();
    e.description = r.description();
    e.kind = r.kind();
    e.category = categories.findById(r.categoryId()).orElseThrow(() -> missing("Category"));
    e.amount = r.amount();
    e.currency = r.currency();
    e.frequency = r.frequency();
    e.customInterval = r.frequency() == BillingFrequency.CUSTOM ? r.customInterval() : null;
    e.customUnit = r.frequency() == BillingFrequency.CUSTOM ? r.customUnit() : null;
    e.startDate = r.startDate();
    e.nextDueDate = r.nextDueDate();
    e.endDate = r.endDate();
    e.paymentMethod = r.paymentMethod();
    e.websiteUrl = r.websiteUrl();
    e.status = r.status();
    e.variableAmount = r.variableAmount();
    e.notes = r.notes();
    expenses.saveAndFlush(e);
    if (id == null) {
      ReminderRule rule = new ReminderRule();
      rule.expense = e;
      rule.daysBefore = settingEntity().reminderDays;
      rules.save(rule);
    }
    return view(e);
  }

  public void delete(long id) {
    Expense e = expenses.findForPayment(id).orElseThrow(() -> missing("Expense"));
    expenses.delete(e);
    expenses.flush();
  }

  public List<CategoryView> categories() {
    return categories.findAllByOrderByNameAsc().stream()
        .map(c -> new CategoryView(c.id, c.name, c.color))
        .toList();
  }

  public CategoryView category(CategoryRequest r) {
    Category c = new Category();
    c.name = r.name().trim();
    c.color = r.color();
    categories.saveAndFlush(c);
    return new CategoryView(c.id, c.name, c.color);
  }

  public List<PaymentView> payments() {
    return payments.findAllByOrderByPaymentDateDescIdDesc().stream()
        .map(this::paymentView)
        .toList();
  }

  public PaymentView pay(long id, PaymentRequest r) {
    // Serialize concurrent submissions before checking the database occurrence constraint.
    Expense e = expenses.findForPayment(id).orElseThrow(() -> missing("Expense"));
    if (payments.existsByExpenseIdAndOccurrenceDate(id, r.occurrenceDate()))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "This occurrence has already been paid");
    require(e.status == ExpenseStatus.ACTIVE, "Only active expenses can be paid");
    require(
        e.nextDueDate.equals(r.occurrenceDate()), "Payment must match the current due occurrence");
    Payment p = new Payment();
    p.expense = e;
    p.occurrenceDate = r.occurrenceDate();
    p.paymentDate = r.paymentDate();
    p.amount = r.amount();
    p.currency = e.currency;
    p.notes = r.notes();
    payments.saveAndFlush(p);
    LocalDate next = billing.next(e, e.nextDueDate);
    if (e.endDate != null && next.isAfter(e.endDate)) e.status = ExpenseStatus.CANCELED;
    else e.nextDueDate = next;
    return paymentView(p);
  }

  public ReminderView reminder(long id) {
    entity(id);
    ReminderRule r = rules.findByExpenseId(id).orElseThrow(() -> missing("Reminder"));
    return new ReminderView(id, r.daysBefore, r.enabled);
  }

  public ReminderView reminder(long id, ReminderRequest request) {
    entity(id);
    ReminderRule r = rules.findByExpenseId(id).orElseThrow(() -> missing("Reminder"));
    r.daysBefore = request.daysBefore();
    r.enabled = request.enabled();
    return new ReminderView(id, r.daysBefore, r.enabled);
  }

  public void refreshNotifications() {
    LocalDate today = LocalDate.now();
    for (ReminderRule r : rules.findAll()) {
      Expense e = r.expense;
      if (r.enabled
          && active(e)
          && !e.nextDueDate.isAfter(today.plusDays(r.daysBefore))
          && !notifications.existsByExpenseIdAndOccurrenceDate(e.id, e.nextDueDate)) {
        Notification n = new Notification();
        n.expense = e;
        n.occurrenceDate = e.nextDueDate;
        n.message = e.name + " is due " + e.nextDueDate;
        notifications.saveAndFlush(n);
      }
    }
  }

  public List<NotificationView> notifications() {
    return notifications.findAllByOrderByCreatedAtDesc().stream()
        .map(n ->
            new NotificationView(
                n.id, n.expense.id, n.message, n.occurrenceDate, n.read, n.createdAt))
        .toList();
  }

  public void readNotification(long id) {
    notifications.findById(id).orElseThrow(() -> missing("Notification")).read = true;
  }

  private AppSettings settingEntity() {
    return settings.findById(1L).orElseThrow(() -> missing("Settings"));
  }

  public SettingsView settings() {
    AppSettings s = settingEntity();
    return new SettingsView(s.currency, s.theme, s.reminderDays);
  }

  public SettingsView settings(SettingsRequest r) {
    try {
      Currency.getInstance(r.currency());
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown currency");
    }
    AppSettings s = settingEntity();
    s.currency = r.currency();
    s.theme = r.theme();
    s.reminderDays = r.reminderDays();
    return settings();
  }

  private boolean active(Expense e) {
    return e.status == ExpenseStatus.ACTIVE;
  }

  private BigDecimal rounded(BigDecimal x) {
    return x.setScale(2, RoundingMode.HALF_UP);
  }

  public Dashboard dashboard() {
    LocalDate today = LocalDate.now();
    List<Expense> active =
        expenses.findAllByOrderByNextDueDateAsc().stream().filter(this::active).toList();
    List<Expense> projected =
        active.stream().filter(e -> e.endDate == null || !e.endDate.isBefore(today)).toList();
    List<Payment> paid = payments.findAllByOrderByPaymentDateDescIdDesc();
    Set<String> currencies = new TreeSet<>();
    active.forEach(e -> currencies.add(e.currency));
    paid.forEach(p -> currencies.add(p.currency));
    List<MoneyTotal> totals =
        currencies.stream()
            .map(c -> {
              BigDecimal monthly =
                  projected.stream()
                      .filter(e -> e.currency.equals(c))
                      .map(billing::monthly)
                      .reduce(BigDecimal.ZERO, BigDecimal::add);
              BigDecimal actual =
                  paid.stream()
                      .filter(p ->
                          p.currency.equals(c)
                              && YearMonth.from(p.paymentDate).equals(YearMonth.from(today)))
                      .map(p -> p.amount)
                      .reduce(BigDecimal.ZERO, BigDecimal::add);
              return new MoneyTotal(
                  c,
                  rounded(monthly),
                  rounded(monthly.multiply(BigDecimal.valueOf(12))),
                  rounded(actual));
            })
            .toList();
    record CategoryKey(String name, String currency) {}
    Map<CategoryKey, BigDecimal> cat = new LinkedHashMap<>();
    for (Expense e : projected)
      cat.merge(new CategoryKey(e.category.name, e.currency), billing.monthly(e), BigDecimal::add);
    List<CategoryTotal> breakdown =
        cat.entrySet().stream()
            .map(x -> {
              CategoryKey key = x.getKey();
              return new CategoryTotal(key.name(), key.currency(), rounded(x.getValue()));
            })
            .toList();
    List<Trend> trends = new ArrayList<>();
    for (int i = 5; i >= 0; i--) {
      YearMonth month = YearMonth.from(today).minusMonths(i);
      for (String c : currencies) {
        BigDecimal sum =
            paid.stream()
                .filter(p -> p.currency.equals(c) && YearMonth.from(p.paymentDate).equals(month))
                .map(p -> p.amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        trends.add(new Trend(month.toString(), c, rounded(sum)));
      }
    }
    return new Dashboard(
        active.size(),
        totals,
        due(active, today, today.plusDays(7)),
        due(active, today, today.plusDays(30)),
        active.stream().filter(e -> e.nextDueDate.isBefore(today)).map(this::view).toList(),
        breakdown,
        projected.stream()
            .sorted(
                Comparator.comparing((Expense e) -> e.currency)
                    .thenComparing(billing::monthly, Comparator.reverseOrder()))
            .limit(5)
            .map(this::view)
            .toList(),
        paid.stream().limit(8).map(this::paymentView).toList(),
        trends);
  }

  private List<ExpenseView> due(List<Expense> list, LocalDate from, LocalDate to) {
    return list.stream()
        .filter(e -> !e.nextDueDate.isBefore(from) && !e.nextDueDate.isAfter(to))
        .map(this::view)
        .toList();
  }

  public List<Map<String, Object>> calendar(LocalDate from, LocalDate to) {
    require(
        !to.isBefore(from) && !to.isAfter(from.plusYears(1)),
        "Calendar range must be between 0 and 366 days");
    List<Map<String, Object>> result = new ArrayList<>();
    for (Expense e : expenses.findAllByOrderByNextDueDateAsc())
      if (e.status == ExpenseStatus.ACTIVE) {
        LocalDate date = e.nextDueDate;
        int iterations = 0;
        while (!date.isAfter(to) && (e.endDate == null || !date.isAfter(e.endDate))) {
          if (++iterations > 20000)
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Expense schedule is too old; update its next due date");
          if (!date.isBefore(from))
            result.add(
                Map.of(
                    "expenseId",
                    e.id,
                    "name",
                    e.name,
                    "date",
                    date,
                    "amount",
                    e.amount,
                    "currency",
                    e.currency));
          date = billing.next(e, date);
        }
      }
    result.sort(Comparator.comparing(m -> (LocalDate) m.get("date")));
    return result;
  }

  public List<ReportTotal> report(int year, int month) {
    require(
        year >= 1900 && year <= 2200 && month >= 1 && month <= 12, "Invalid report year or month");
    List<Payment> records = payments.findAllByOrderByPaymentDateDescIdDesc();
    Set<String> currencies = new TreeSet<>();
    records.forEach(p -> currencies.add(p.currency));
    return currencies.stream()
        .map(currency -> {
          BigDecimal monthTotal = BigDecimal.ZERO;
          BigDecimal yearTotal = BigDecimal.ZERO;
          for (Payment p : records)
            if (p.currency.equals(currency) && p.paymentDate.getYear() == year) {
              yearTotal = yearTotal.add(p.amount);
              if (p.paymentDate.getMonthValue() == month) monthTotal = monthTotal.add(p.amount);
            }
          return new ReportTotal(currency, rounded(monthTotal), rounded(yearTotal));
        })
        .toList();
  }

  public String csv(LocalDate from, LocalDate to) {
    require(!to.isBefore(from), "End must be on or after start");
    StringBuilder s =
        new StringBuilder("Payment ID,Expense,Occurrence,Payment date,Amount,Currency,Notes\r\n");
    for (Payment p : payments.findAllByOrderByPaymentDateDescIdDesc())
      if (!p.paymentDate.isBefore(from) && !p.paymentDate.isAfter(to))
        s.append(p.id)
            .append(',')
            .append(csvCell(p.expense.name))
            .append(',')
            .append(p.occurrenceDate)
            .append(',')
            .append(p.paymentDate)
            .append(',')
            .append(p.amount)
            .append(',')
            .append(p.currency)
            .append(',')
            .append(csvCell(p.notes))
            .append("\r\n");
    return s.toString();
  }

  private String csvCell(String value) {
    if (value == null) return "\"\"";
    String leading = value.stripLeading();
    if (!leading.isEmpty() && "=+-@".indexOf(leading.charAt(0)) >= 0) value = "'" + value;
    return "\"" + value.replace("\"", "\"\"") + "\"";
  }

  private PaymentView paymentView(Payment p) {
    return new PaymentView(
        p.id,
        p.expense.id,
        p.expense.name,
        p.occurrenceDate,
        p.paymentDate,
        p.amount,
        p.currency,
        p.notes);
  }

  public ExpenseView view(Expense e) {
    return new ExpenseView(
        e.id,
        e.name,
        e.description,
        e.kind,
        e.category.id,
        e.category.name,
        e.category.color,
        e.amount,
        e.currency,
        e.frequency,
        e.customInterval,
        e.customUnit,
        e.startDate,
        e.nextDueDate,
        e.endDate,
        e.paymentMethod,
        e.websiteUrl,
        e.status,
        e.variableAmount,
        e.notes,
        e.version,
        e.createdAt,
        e.updatedAt);
  }
}
