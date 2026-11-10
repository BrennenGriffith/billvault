package com.brennensubs.billvault.desktop;

import com.brennensubs.billvault.dto.Api.*;
import com.brennensubs.billvault.model.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import javafx.application.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.util.StringConverter;

public final class DesktopApplication extends Application {
  private final ExecutorService worker =
      Executors.newSingleThreadExecutor(r -> new Thread(r, "billvault-storage"));
  private volatile DesktopStore store;
  private Stage stage;
  private final BorderPane root = new BorderPane();
  private final Label status = new Label("Opening your local vault…");
  private final Label overview = new Label();
  private final TableView<ExpenseView> expenses = new TableView<>();
  private final TableView<PaymentView> payments = new TableView<>();
  private final TableView<NotificationView> notices = new TableView<>();
  private final TextField search = new TextField();
  private final ComboBox<ExpenseStatus> filter = new ComboBox<>();
  private final Label pageInfo = new Label();
  private final Label reportOutput = new Label();
  private final VBox dashboard = new VBox(18);
  private List<CategoryView> categories = List.of();
  private Path directory;
  private int page;
  private int pages;
  private String defaultCurrency = "USD";

  @Override
  public void start(Stage primary) {
    stage = primary;
    directory =
        getParameters().getNamed().containsKey("data-dir")
            ? Path.of(getParameters().getNamed().get("data-dir"))
            : DesktopStore.defaultDirectory();
    Label brand = new Label("BillVault  /  Desktop");
    brand.getStyleClass().add("brand");
    Label privacy = new Label("LOCAL STORAGE · NO HOSTING REQUIRED");
    privacy.getStyleClass().add("muted");
    VBox heading = new VBox(6, brand, privacy);
    heading.setPadding(new Insets(24));
    root.setTop(heading);
    root.setCenter(new Label("Starting local database…"));
    status.setPadding(new Insets(12, 24, 12, 24));
    root.setBottom(status);
    Scene scene = new Scene(root, 1160, 780);
    scene
        .getStylesheets()
        .add(Objects.requireNonNull(getClass().getResource("desktop.css")).toExternalForm());
    stage.setTitle("BillVault Desktop");
    stage
        .getIcons()
        .add(
            new javafx.scene.image.Image(
                Objects.requireNonNull(getClass().getResourceAsStream("billvault.png"))));
    stage.setScene(scene);
    stage.setMinWidth(820);
    stage.setMinHeight(580);
    stage.show();
    work(
        () -> {
          store = new DesktopStore(directory);
          return store;
        },
        opened -> {
          build();
          refresh();
        });
  }

  private void build() {
    TabPane tabs = new TabPane();
    tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    dashboard.setPadding(new Insets(24));
    overview.getStyleClass().add("summary");
    overview.setWrapText(true);
    dashboard.getChildren().add(overview);
    tabs.getTabs().add(new Tab("Overview", new ScrollPane(dashboard)));
    configureExpenses();
    search.setPromptText("Search expense names");
    search.setPrefWidth(230);
    search.setOnAction(
        e -> {
          page = 0;
          refresh();
        });
    filter.getItems().addAll(ExpenseStatus.values());
    filter.setPromptText("All statuses");
    filter.setOnAction(
        e -> {
          page = 0;
          refresh();
        });
    HBox tools =
        new HBox(
            8,
            search,
            filter,
            button(
                "Search",
                () -> {
                  page = 0;
                  refresh();
                }),
            button(
                "Clear",
                () -> {
                  search.clear();
                  filter.setValue(null);
                  page = 0;
                  refresh();
                }),
            button("Add expense", () -> edit(null)),
            button("Add category", this::category));
    tools.setPadding(new Insets(16));
    HBox actions =
        new HBox(
            8,
            button("Edit selected", () -> selected(this::edit)),
            button("Record payment", () -> selected(this::pay)),
            button("Reminder", () -> selected(this::reminder)),
            button("Delete", () -> selected(this::delete)),
            button(
                "Previous",
                () -> {
                  if (page > 0) {
                    page--;
                    refresh();
                  }
                }),
            pageInfo,
            button(
                "Next",
                () -> {
                  if (page + 1 < pages) {
                    page++;
                    refresh();
                  }
                }));
    actions.setPadding(new Insets(16));
    BorderPane expensePane = new BorderPane(expenses, tools, null, actions, null);
    tabs.getTabs().add(new Tab("Expenses", expensePane));
    column(payments, "Expense", PaymentView::expenseName);
    column(payments, "Paid on", x -> x.paymentDate().toString());
    column(payments, "Occurrence", x -> x.occurrenceDate().toString());
    column(payments, "Amount", x -> money(x.currency(), x.amount()));
    column(payments, "Notes", PaymentView::notes);
    tabs.getTabs().add(new Tab("Payments", payments));
    DatePicker month = new DatePicker(LocalDate.now());
    DatePicker from = new DatePicker(LocalDate.now().withDayOfYear(1));
    DatePicker to = new DatePicker(LocalDate.now());
    reportOutput.setWrapText(true);
    VBox reports =
        new VBox(
            18,
            new Label("Actual payments by month and year"),
            new HBox(
                10,
                month,
                button(
                    "Show totals",
                    () -> {
                      LocalDate d = month.getValue();
                      if (d == null) {
                        error("Choose a report date.");
                        return;
                      }
                      work(
                          () -> store.service.report(d.getYear(), d.getMonthValue()),
                          rows ->
                              reportOutput.setText(
                                  rows.isEmpty()
                                      ? "No payments in this vault."
                                      : String.join(
                                          "\n",
                                          rows.stream()
                                              .map(
                                                  r ->
                                                      r.currency()
                                                          + "  ·  Month: "
                                                          + r.paidInMonth()
                                                          + "  ·  Year: "
                                                          + r.paidInYear())
                                              .toList())));
                    })),
            reportOutput,
            new Separator(),
            new Label("Export payment history as CSV"),
            new HBox(
                10,
                new Label("From"),
                from,
                new Label("To"),
                to,
                button("Export CSV", () -> export(from.getValue(), to.getValue()))));
    reports.setPadding(new Insets(24));
    tabs.getTabs().add(new Tab("Reports & export", reports));
    column(notices, "Reminder", NotificationView::message);
    column(notices, "Due", x -> x.occurrenceDate().toString());
    column(notices, "Read", x -> x.read() ? "Yes" : "No");
    HBox noticeTools =
        new HBox(
            10,
            button("Refresh reminders", this::refresh),
            button(
                "Mark selected read",
                () -> {
                  var n = notices.getSelectionModel().getSelectedItem();
                  if (n != null)
                    work(
                        () -> {
                          store.service.readNotification(n.id());
                          return true;
                        },
                        x -> refresh());
                }));
    noticeTools.setPadding(new Insets(16));
    tabs.getTabs()
        .add(new Tab("Reminders", new BorderPane(notices, noticeTools, null, null, null)));
    Label location = new Label(directory.toAbsolutePath().normalize().toString());
    location.setWrapText(true);
    Label backup =
        new Label(
            "To back up: close BillVault, then copy billvault.mv.db from the folder above to"
                + " another device.\n"
                + "To restore: close BillVault, preserve your current file, then copy the verified"
                + " backup into this folder.\n"
                + "The database is not encrypted. Your operating-system account protects access.\n"
                + "This desktop vault is separate from the web edition; there is no automatic"
                + " synchronization.");
    backup.setWrapText(true);
    VBox settings =
        new VBox(
            18,
            new Label("Your database folder"),
            location,
            new Separator(),
            backup,
            button("Preferences", this::preferences),
            new Separator(),
            new Label(
                "BillVault Desktop · MIT License · Built with JavaFX and Spring Boot\n"
                    + "No account, cloud service, bank connection, or saved administrator"
                    + " credentials."));
    settings.setPadding(new Insets(24));
    tabs.getTabs().add(new Tab("Storage & preferences", settings));
    root.setCenter(tabs);
  }

  private void configureExpenses() {
    column(expenses, "Name", ExpenseView::name);
    column(expenses, "Kind", x -> x.kind().toString());
    column(expenses, "Category", ExpenseView::categoryName);
    column(expenses, "Amount", x -> money(x.currency(), x.amount()));
    column(expenses, "Frequency", x -> x.frequency().toString());
    column(expenses, "Next due", x -> x.nextDueDate().toString());
    column(expenses, "Status", x -> x.status().toString());
    expenses.setRowFactory(
        t -> {
          TableRow<ExpenseView> row = new TableRow<>();
          row.setOnMouseClicked(
              e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) edit(row.getItem());
              });
          return row;
        });
  }

  private record Snapshot(
      Page<ExpenseView> expenses,
      List<PaymentView> payments,
      Dashboard dashboard,
      List<CategoryView> categories,
      List<NotificationView> notices,
      SettingsView settings) {}

  private void refresh() {
    String query = search.getText();
    ExpenseStatus state = filter.getValue();
    int requested = page;
    work(
        () -> {
          store.service.refreshNotifications();
          return new Snapshot(
              store.service.list(query, null, state, null, "nextDueDate", requested, 50),
              store.service.payments(),
              store.service.dashboard(),
              store.service.categories(),
              store.service.notifications(),
              store.service.settings());
        },
        s -> {
          defaultCurrency = s.settings().currency();
          categories = s.categories();
          expenses.getItems().setAll(s.expenses().content());
          payments.getItems().setAll(s.payments());
          notices.getItems().setAll(s.notices());
          pages = s.expenses().totalPages();
          pageInfo.setText(
              "Page "
                  + (page + 1)
                  + " / "
                  + Math.max(1, pages)
                  + " · "
                  + s.expenses().totalElements()
                  + " expenses");
          var d = s.dashboard();
          overview.setText(
              "Your money, in view.\n\n"
                  + d.activeCount()
                  + " active expenses · "
                  + d.overdue().size()
                  + " overdue\n\n"
                  + (d.totals().isEmpty()
                      ? "Add your first bill or subscription to begin."
                      : String.join(
                          "\n",
                          d.totals().stream()
                              .map(
                                  t ->
                                      t.currency()
                                          + "  ·  Monthly estimate "
                                          + t.monthly()
                                          + "  ·  Annual estimate "
                                          + t.annual()
                                          + "  ·  Paid this month "
                                          + t.paidThisMonth())
                              .toList())));
          dashboard
              .getChildren()
              .setAll(overview, new Separator(), new Label("DUE IN THE NEXT 30 DAYS"));
          if (d.upcoming30().isEmpty()) dashboard.getChildren().add(new Label("Nothing upcoming."));
          d.upcoming30()
              .forEach(
                  e ->
                      dashboard
                          .getChildren()
                          .add(
                              new Label(
                                  e.nextDueDate()
                                      + "   "
                                      + e.name()
                                      + "   "
                                      + money(e.currency(), e.amount()))));
          dashboard
              .getChildren()
              .addAll(new Separator(), new Label("MONTHLY ESTIMATE BY CATEGORY"));
          d.categories()
              .forEach(
                  c ->
                      dashboard
                          .getChildren()
                          .add(new Label(c.category() + "   " + money(c.currency(), c.monthly()))));
          status.setText("Saved locally · " + directory.toAbsolutePath().normalize());
          if ("true".equals(getParameters().getNamed().get("smoke-test"))) {
            var finish = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
            finish.setOnFinished(
                event -> {
                  stage.close();
                  Platform.exit();
                });
            finish.play();
          }
        });
  }

  private void edit(ExpenseView original) {
    Dialog<ButtonType> dialog = dialog(original == null ? "Add expense" : "Edit expense");
    GridPane grid = grid();
    TextField name = new TextField(original == null ? "" : original.name());
    TextField description = new TextField(original == null ? "" : original.description());
    ComboBox<ExpenseKind> kind =
        combo(ExpenseKind.values(), original == null ? ExpenseKind.SUBSCRIPTION : original.kind());
    ComboBox<CategoryView> category = new ComboBox<>(FXCollections.observableArrayList(categories));
    category.setConverter(
        new StringConverter<>() {
          public String toString(CategoryView c) {
            return c == null ? "" : c.name();
          }

          public CategoryView fromString(String s) {
            return null;
          }
        });
    category.setValue(
        categories.stream()
            .filter(c -> original == null || c.id().equals(original.categoryId()))
            .findFirst()
            .orElse(null));
    TextField amount = new TextField(original == null ? "" : original.amount().toPlainString());
    TextField currency = new TextField(original == null ? defaultCurrency : original.currency());
    ComboBox<BillingFrequency> frequency =
        combo(
            BillingFrequency.values(),
            original == null ? BillingFrequency.MONTHLY : original.frequency());
    TextField interval =
        new TextField(
            original == null || original.customInterval() == null
                ? "1"
                : original.customInterval().toString());
    ComboBox<IntervalUnit> unit =
        combo(
            IntervalUnit.values(),
            original == null || original.customUnit() == null
                ? IntervalUnit.MONTHS
                : original.customUnit());
    DatePicker start = new DatePicker(original == null ? LocalDate.now() : original.startDate());
    DatePicker due = new DatePicker(original == null ? LocalDate.now() : original.nextDueDate());
    DatePicker end = new DatePicker(original == null ? null : original.endDate());
    TextField method = new TextField(original == null ? "" : original.paymentMethod());
    TextField website = new TextField(original == null ? "" : original.websiteUrl());
    ComboBox<ExpenseStatus> state =
        combo(ExpenseStatus.values(), original == null ? ExpenseStatus.ACTIVE : original.status());
    CheckBox variable = new CheckBox("Variable amount");
    variable.setSelected(original != null && original.variableAmount());
    TextArea notes = new TextArea(original == null ? "" : original.notes());
    notes.setPrefRowCount(3);
    Object[][] fields = {
      {"Name", name},
      {"Description", description},
      {"Kind", kind},
      {"Category", category},
      {"Amount", amount},
      {"Currency", currency},
      {"Frequency", frequency},
      {"Custom interval", interval},
      {"Custom unit", unit},
      {"Start date", start},
      {"Next due date", due},
      {"End date (optional)", end},
      {"Payment label", method},
      {"Website (optional)", website},
      {"Status", state},
      {"", variable},
      {"Notes", notes}
    };
    for (int i = 0; i < fields.length; i++)
      grid.addRow(i, new Label((String) fields[i][0]), (Node) fields[i][1]);
    ScrollPane scroll = new ScrollPane(grid);
    scroll.setFitToWidth(true);
    scroll.setPrefViewportHeight(510);
    dialog.getDialogPane().setContent(scroll);
    dialog.getDialogPane().setPrefWidth(560);
    onSave(
        dialog,
        () -> {
          if (category.getValue() == null)
            throw new IllegalArgumentException("Choose a category first.");
          ExpenseRequest request =
              store.validated(
                  new ExpenseRequest(
                      name.getText(),
                      description.getText(),
                      kind.getValue(),
                      category.getValue().id(),
                      new BigDecimal(amount.getText()),
                      currency.getText().trim().toUpperCase(Locale.ROOT),
                      frequency.getValue(),
                      frequency.getValue() == BillingFrequency.CUSTOM
                          ? Integer.valueOf(interval.getText())
                          : null,
                      frequency.getValue() == BillingFrequency.CUSTOM ? unit.getValue() : null,
                      start.getValue(),
                      due.getValue(),
                      end.getValue(),
                      method.getText(),
                      website.getText(),
                      state.getValue(),
                      variable.isSelected(),
                      notes.getText(),
                      original == null ? null : original.version()));
          work(
              () -> store.service.save(original == null ? null : original.id(), request),
              e -> refresh());
        });
    dialog.showAndWait();
  }

  private void pay(ExpenseView e) {
    Dialog<ButtonType> dialog = dialog("Record payment · " + e.name());
    GridPane grid = grid();
    TextField amount = new TextField(e.amount().toPlainString());
    DatePicker date = new DatePicker(LocalDate.now());
    TextArea notes = new TextArea();
    notes.setPrefRowCount(3);
    grid.addRow(0, new Label("Due occurrence"), new Label(e.nextDueDate().toString()));
    grid.addRow(1, new Label("Paid on"), date);
    grid.addRow(2, new Label("Amount (" + e.currency() + ")"), amount);
    grid.addRow(3, new Label("Notes"), notes);
    dialog.getDialogPane().setContent(grid);
    onSave(
        dialog,
        () -> {
          var request =
              store.validated(
                  new PaymentRequest(
                      e.nextDueDate(),
                      date.getValue(),
                      new BigDecimal(amount.getText()),
                      notes.getText()));
          work(() -> store.service.pay(e.id(), request), p -> refresh());
        });
    dialog.showAndWait();
  }

  private void category() {
    Dialog<ButtonType> dialog = dialog("Add category");
    TextField name = new TextField();
    ColorPicker color = new ColorPicker(javafx.scene.paint.Color.web("#7161ef"));
    GridPane grid = grid();
    grid.addRow(0, new Label("Name"), name);
    grid.addRow(1, new Label("Color"), color);
    dialog.getDialogPane().setContent(grid);
    onSave(
        dialog,
        () -> {
          var c = color.getValue();
          var r =
              store.validated(
                  new CategoryRequest(
                      name.getText(),
                      String.format(
                          "#%02x%02x%02x",
                          Math.round(c.getRed() * 255),
                          Math.round(c.getGreen() * 255),
                          Math.round(c.getBlue() * 255))));
          work(() -> store.service.category(r), x -> refresh());
        });
    dialog.showAndWait();
  }

  private void reminder(ExpenseView e) {
    work(
        () -> store.service.reminder(e.id()),
        r -> {
          Dialog<ButtonType> d = dialog("Reminder · " + e.name());
          Spinner<Integer> days = new Spinner<>(0, 90, r.daysBefore());
          CheckBox enabled = new CheckBox("Enabled");
          enabled.setSelected(r.enabled());
          d.getDialogPane().setContent(new VBox(12, new Label("Days before due"), days, enabled));
          onSave(
              d,
              () ->
                  work(
                      () ->
                          store.service.reminder(
                              e.id(),
                              store.validated(
                                  new ReminderRequest(days.getValue(), enabled.isSelected()))),
                      x -> refresh()));
          d.showAndWait();
        });
  }

  private void preferences() {
    work(
        () -> store.service.settings(),
        s -> {
          Dialog<ButtonType> d = dialog("Preferences");
          TextField currency = new TextField(s.currency());
          Spinner<Integer> days = new Spinner<>(0, 90, s.reminderDays());
          d.getDialogPane()
              .setContent(
                  new VBox(
                      12,
                      new Label("Default currency"),
                      currency,
                      new Label("Default reminder lead days"),
                      days));
          onSave(
              d,
              () -> {
                var r =
                    store.validated(
                        new SettingsRequest(
                            currency.getText().trim().toUpperCase(Locale.ROOT),
                            s.theme(),
                            days.getValue()));
                work(() -> store.service.settings(r), x -> refresh());
              });
          d.showAndWait();
        });
  }

  private void delete(ExpenseView e) {
    Alert confirm =
        new Alert(
            Alert.AlertType.CONFIRMATION,
            "Delete “"
                + e.name()
                + "” and all of its payment history? Cancel the expense instead to preserve"
                + " history.",
            ButtonType.CANCEL,
            ButtonType.OK);
    confirm.initOwner(stage);
    confirm.setHeaderText("Permanently delete expense");
    if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK)
      work(
          () -> {
            store.service.delete(e.id());
            return true;
          },
          x -> {
            page = 0;
            refresh();
          });
  }

  private void export(LocalDate from, LocalDate to) {
    if (from == null || to == null || to.isBefore(from)) {
      error("Choose a valid date range.");
      return;
    }
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Export payments");
    chooser.setInitialFileName("billvault-payments.csv");
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
    var file = chooser.showSaveDialog(stage);
    if (file == null) return;
    work(
        () -> {
          Files.writeString(file.toPath(), store.service.csv(from, to), StandardCharsets.UTF_8);
          return file;
        },
        saved -> status.setText("Exported: " + saved.getAbsolutePath()));
  }

  private void selected(Consumer<ExpenseView> action) {
    var e = expenses.getSelectionModel().getSelectedItem();
    if (e == null) error("Select an expense first.");
    else action.accept(e);
  }

  private static String money(String c, BigDecimal amount) {
    return c + " " + amount.toPlainString();
  }

  private static Button button(String text, Runnable action) {
    Button b = new Button(text);
    b.setOnAction(e -> action.run());
    return b;
  }

  private static GridPane grid() {
    GridPane g = new GridPane();
    g.setHgap(14);
    g.setVgap(12);
    g.setPadding(new Insets(18));
    return g;
  }

  private static <E> ComboBox<E> combo(E[] values, E selected) {
    ComboBox<E> c = new ComboBox<>(FXCollections.observableArrayList(values));
    c.setValue(selected);
    return c;
  }

  private static <T> void column(TableView<T> table, String title, Function<T, String> value) {
    TableColumn<T, String> c = new TableColumn<>(title);
    c.setCellValueFactory(x -> new SimpleStringProperty(value.apply(x.getValue())));
    c.setPrefWidth(
        title.equals("Name") || title.equals("Expense") || title.equals("Reminder") ? 220 : 130);
    table.getColumns().add(c);
    table.setPlaceholder(new Label("No records yet."));
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
  }

  private Dialog<ButtonType> dialog(String title) {
    Dialog<ButtonType> d = new Dialog<>();
    d.initOwner(stage);
    d.setTitle(title);
    d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
    return d;
  }

  private void onSave(Dialog<ButtonType> dialog, Runnable save) {
    dialog
        .getDialogPane()
        .lookupButton(ButtonType.OK)
        .addEventFilter(
            javafx.event.ActionEvent.ACTION,
            event -> {
              try {
                save.run();
              } catch (Exception ex) {
                event.consume();
                error(message(ex));
              }
            });
  }

  @FunctionalInterface
  private interface Job<T> {
    T run() throws Exception;
  }

  private <T> void work(Job<T> job, Consumer<T> success) {
    root.setDisable(true);
    status.setText("Working…");
    worker.submit(
        () -> {
          try {
            T result = job.run();
            Platform.runLater(
                () -> {
                  root.setDisable(false);
                  try {
                    success.accept(result);
                  } catch (Exception ex) {
                    error(message(ex));
                  }
                });
          } catch (Exception ex) {
            Platform.runLater(
                () -> {
                  root.setDisable(false);
                  status.setText("Operation failed; your existing records were preserved.");
                  error(message(ex));
                });
          }
        });
  }

  private static String message(Throwable ex) {
    if (ex instanceof org.springframework.web.server.ResponseStatusException r)
      return r.getReason();
    if (ex instanceof jakarta.validation.ConstraintViolationException v)
      return String.join(
          "\n",
          v.getConstraintViolations().stream()
              .map(c -> c.getPropertyPath() + ": " + c.getMessage())
              .sorted()
              .toList());
    if (ex instanceof org.springframework.dao.DataIntegrityViolationException)
      return "This conflicts with an existing record. Category names must be unique.";
    return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
  }

  private void error(String text) {
    Alert a = new Alert(Alert.AlertType.ERROR, text, ButtonType.OK);
    a.initOwner(stage);
    a.setHeaderText("BillVault could not complete that action");
    a.showAndWait();
  }

  @Override
  public void stop() {
    worker.submit(
        () -> {
          if (store != null) store.close();
        });
    worker.shutdown();
  }
}
