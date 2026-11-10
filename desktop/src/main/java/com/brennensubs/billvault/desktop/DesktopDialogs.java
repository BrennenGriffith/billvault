package com.brennensubs.billvault.desktop;

import com.brennensubs.billvault.dto.Api.CategoryRequest;
import com.brennensubs.billvault.dto.Api.CategoryView;
import com.brennensubs.billvault.dto.Api.ExpenseRequest;
import com.brennensubs.billvault.dto.Api.ExpenseView;
import com.brennensubs.billvault.dto.Api.PaymentRequest;
import com.brennensubs.billvault.dto.Api.ReminderRequest;
import com.brennensubs.billvault.dto.Api.ReminderView;
import com.brennensubs.billvault.model.BillingFrequency;
import com.brennensubs.billvault.model.ExpenseKind;
import com.brennensubs.billvault.model.ExpenseStatus;
import com.brennensubs.billvault.model.IntervalUnit;
import jakarta.validation.ConstraintViolationException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

/** Modal dialogs for creating and editing expenses, payments, categories, and reminders. */
final class DesktopDialogs {
  private DesktopDialogs() {}

  static void showExpenseDialog(
      Stage owner,
      boolean darkTheme,
      DesktopStore store,
      ExpenseView original,
      ExpenseKind defaultKind,
      String defaultCurrency,
      List<CategoryView> categories,
      Consumer<ExpenseRequest> onSubmit) {
    Dialog<ButtonType> dialog =
        baseDialog(
            owner,
            darkTheme,
            original == null ? "Add recurring expense" : "Edit expense · " + original.name());

    Label errorBanner = inlineErrorBanner();

    TextField name = new TextField(original == null ? "" : original.name());
    name.setPromptText("e.g. Streambox, Home fiber, Electricity");

    ComboBox<ExpenseKind> kind =
        enumCombo(
            ExpenseKind.values(),
            original != null
                ? original.kind()
                : (defaultKind != null ? defaultKind : ExpenseKind.SUBSCRIPTION));

    ComboBox<CategoryView> category =
        new ComboBox<>(FXCollections.observableArrayList(categories));
    category.setMaxWidth(Double.MAX_VALUE);
    category.setConverter(
        new StringConverter<>() {
          @Override
          public String toString(CategoryView c) {
            return c == null ? "" : c.name();
          }

          @Override
          public CategoryView fromString(String s) {
            return null;
          }
        });
    category.setValue(
        categories.stream()
            .filter(c -> original == null || c.id().equals(original.categoryId()))
            .findFirst()
            .orElse(null));

    ComboBox<ExpenseStatus> state =
        enumCombo(
            ExpenseStatus.values(), original == null ? ExpenseStatus.ACTIVE : original.status());

    TextField amount =
        new TextField(original == null ? "" : original.amount().toPlainString());
    amount.setPromptText("0.00");

    TextField currency =
        new TextField(original == null ? defaultCurrency : original.currency());
    currency.setPromptText("USD");

    ComboBox<BillingFrequency> frequency =
        enumCombo(
            BillingFrequency.values(),
            original == null ? BillingFrequency.MONTHLY : original.frequency());

    TextField interval =
        new TextField(
            original == null || original.customInterval() == null
                ? "1"
                : original.customInterval().toString());
    ComboBox<IntervalUnit> unit =
        enumCombo(
            IntervalUnit.values(),
            original == null || original.customUnit() == null
                ? IntervalUnit.MONTHS
                : original.customUnit());

    DatePicker start =
        new DatePicker(original == null ? LocalDate.now() : original.startDate());
    start.setMaxWidth(Double.MAX_VALUE);
    DatePicker due =
        new DatePicker(original == null ? LocalDate.now() : original.nextDueDate());
    due.setMaxWidth(Double.MAX_VALUE);
    DatePicker end = new DatePicker(original == null ? null : original.endDate());
    end.setMaxWidth(Double.MAX_VALUE);
    end.setPromptText("Optional");

    TextField method = new TextField(original == null ? "" : original.paymentMethod());
    method.setPromptText("e.g. Visa •••• 4242, Auto-debit");
    TextField website = new TextField(original == null ? "" : original.websiteUrl());
    website.setPromptText("https://example.com");
    TextField description = new TextField(original == null ? "" : original.description());
    description.setPromptText("Short summary or plan tier");

    CheckBox variable = new CheckBox("Variable amount (estimate may differ from actual bill)");
    variable.setSelected(original != null && original.variableAmount());

    TextArea notes = new TextArea(original == null ? "" : original.notes());
    notes.setPrefRowCount(3);
    notes.setPromptText("Optional notes, account number, or cancellation instructions");

    GridPane form = twoColumnGrid();
    int row = 0;
    form.add(fieldBlock("Expense name", name), 0, row, 2, 1);
    row++;
    form.add(fieldBlock("Kind", kind), 0, row);
    form.add(fieldBlock("Category", category), 1, row);
    row++;
    form.add(fieldBlock("Amount", amount), 0, row);
    form.add(fieldBlock("Currency (ISO 3-letter)", currency), 1, row);
    row++;
    form.add(fieldBlock("Billing frequency", frequency), 0, row);
    form.add(fieldBlock("Status", state), 1, row);
    row++;

    VBox customIntervalBlock = fieldBlock("Custom interval", interval);
    VBox customUnitBlock = fieldBlock("Custom unit", unit);
    form.add(customIntervalBlock, 0, row);
    form.add(customUnitBlock, 1, row);
    Runnable syncCustomVisibility =
        () -> {
          boolean isCustom = frequency.getValue() == BillingFrequency.CUSTOM;
          customIntervalBlock.setDisable(!isCustom);
          customUnitBlock.setDisable(!isCustom);
        };
    syncCustomVisibility.run();
    frequency.setOnAction(e -> syncCustomVisibility.run());
    row++;

    form.add(fieldBlock("Start date", start), 0, row);
    form.add(fieldBlock("Next due date", due), 1, row);
    row++;
    form.add(fieldBlock("End date (optional)", end), 0, row);
    form.add(fieldBlock("Payment label (optional)", method), 1, row);
    row++;
    form.add(fieldBlock("Description (optional)", description), 0, row);
    form.add(fieldBlock("Website URL (optional)", website), 1, row);
    row++;
    form.add(variable, 0, row, 2, 1);
    row++;
    form.add(fieldBlock("Notes", notes), 0, row, 2, 1);

    VBox container = new VBox(12, errorBanner, form);
    ScrollPane scroll = new ScrollPane(container);
    scroll.setFitToWidth(true);
    scroll.setPrefViewportHeight(500);
    dialog.getDialogPane().setContent(scroll);
    dialog.getDialogPane().setPrefWidth(620);

    onSave(
        dialog,
        errorBanner,
        () -> {
          if (category.getValue() == null) {
            throw new IllegalArgumentException("Choose a category first.");
          }
          BigDecimal parsedAmount;
          try {
            parsedAmount = new BigDecimal(amount.getText().trim());
          } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Enter a valid decimal amount (for example 14.99).");
          }
          Integer parsedInterval = null;
          if (frequency.getValue() == BillingFrequency.CUSTOM) {
            try {
              parsedInterval = Integer.valueOf(interval.getText().trim());
            } catch (NumberFormatException ex) {
              throw new IllegalArgumentException("Custom interval must be a whole number.");
            }
          }
          ExpenseRequest request =
              store.validated(
                  new ExpenseRequest(
                      name.getText(),
                      description.getText(),
                      kind.getValue(),
                      category.getValue().id(),
                      parsedAmount,
                      currency.getText().trim().toUpperCase(Locale.ROOT),
                      frequency.getValue(),
                      parsedInterval,
                      frequency.getValue() == BillingFrequency.CUSTOM ? unit.getValue() : null,
                      start.getValue(),
                      due.getValue(),
                      end.getValue(),
                      method.getText(),
                      website.getText().trim(),
                      state.getValue(),
                      variable.isSelected(),
                      notes.getText(),
                      original == null ? null : original.version()));
          onSubmit.accept(request);
        });
    dialog.showAndWait();
  }

  static void showPaymentDialog(
      Stage owner,
      boolean darkTheme,
      DesktopStore store,
      ExpenseView expense,
      Consumer<PaymentRequest> onSubmit) {
    Dialog<ButtonType> dialog =
        baseDialog(owner, darkTheme, "Record payment · " + expense.name());
    Label errorBanner = inlineErrorBanner();

    Label info =
        new Label(
            "For the occurrence due "
                + DesktopFormat.dateLabel(expense.nextDueDate())
                + ". Recording a payment updates your ledger and advances the next due date.");
    info.getStyleClass().add("muted-text");
    info.setWrapText(true);

    TextField amount = new TextField(expense.amount().toPlainString());
    DatePicker date = new DatePicker(LocalDate.now());
    date.setMaxWidth(Double.MAX_VALUE);
    TextArea notes = new TextArea();
    notes.setPrefRowCount(3);
    notes.setPromptText("Optional confirmation number or payment note");

    GridPane grid = twoColumnGrid();
    grid.add(fieldBlock("Amount paid (" + expense.currency() + ")", amount), 0, 0);
    grid.add(fieldBlock("Payment date", date), 1, 0);
    grid.add(fieldBlock("Notes", notes), 0, 1, 2, 1);

    VBox content = new VBox(14, errorBanner, info, grid);
    content.setPadding(new Insets(8));
    dialog.getDialogPane().setContent(content);
    dialog.getDialogPane().setPrefWidth(480);

    onSave(
        dialog,
        errorBanner,
        () -> {
          BigDecimal parsed;
          try {
            parsed = new BigDecimal(amount.getText().trim());
          } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Enter a valid decimal payment amount.");
          }
          PaymentRequest req =
              store.validated(
                  new PaymentRequest(
                      expense.nextDueDate(), date.getValue(), parsed, notes.getText()));
          onSubmit.accept(req);
        });
    dialog.showAndWait();
  }

  static void showCategoryDialog(
      Stage owner,
      boolean darkTheme,
      DesktopStore store,
      Consumer<CategoryRequest> onSubmit) {
    Dialog<ButtonType> dialog = baseDialog(owner, darkTheme, "Add expense category");
    Label errorBanner = inlineErrorBanner();

    TextField name = new TextField();
    name.setPromptText("e.g. Utilities, Cloud & AI, Insurance");
    ColorPicker color = new ColorPicker(Color.web("#6056dc"));
    color.setMaxWidth(Double.MAX_VALUE);

    GridPane grid = twoColumnGrid();
    grid.add(fieldBlock("Category name", name), 0, 0);
    grid.add(fieldBlock("Badge color", color), 1, 0);

    VBox content = new VBox(12, errorBanner, grid);
    content.setPadding(new Insets(8));
    dialog.getDialogPane().setContent(content);
    dialog.getDialogPane().setPrefWidth(440);

    onSave(
        dialog,
        errorBanner,
        () -> {
          Color c = color.getValue();
          String hex =
              String.format(
                  "#%02x%02x%02x",
                  Math.round(c.getRed() * 255),
                  Math.round(c.getGreen() * 255),
                  Math.round(c.getBlue() * 255));
          CategoryRequest req = store.validated(new CategoryRequest(name.getText(), hex));
          onSubmit.accept(req);
        });
    dialog.showAndWait();
  }

  static void showReminderDialog(
      Stage owner,
      boolean darkTheme,
      DesktopStore store,
      ExpenseView expense,
      ReminderView current,
      Consumer<ReminderRequest> onSubmit) {
    Dialog<ButtonType> dialog =
        baseDialog(owner, darkTheme, "Reminder · " + expense.name());
    Label errorBanner = inlineErrorBanner();

    Spinner<Integer> days = new Spinner<>(0, 90, current.daysBefore());
    days.setEditable(true);
    days.setMaxWidth(Double.MAX_VALUE);
    CheckBox enabled = new CheckBox("Enable in-app reminder before due date");
    enabled.setSelected(current.enabled());

    VBox content =
        new VBox(
            14,
            errorBanner,
            fieldBlock("Days before payment is due (0–90)", days),
            enabled);
    content.setPadding(new Insets(12));
    dialog.getDialogPane().setContent(content);
    dialog.getDialogPane().setPrefWidth(400);

    onSave(
        dialog,
        errorBanner,
        () -> {
          ReminderRequest req =
              store.validated(new ReminderRequest(days.getValue(), enabled.isSelected()));
          onSubmit.accept(req);
        });
    dialog.showAndWait();
  }

  static boolean confirmDelete(Stage owner, boolean darkTheme, ExpenseView expense) {
    Alert confirm =
        new Alert(
            Alert.AlertType.CONFIRMATION,
            "Delete “"
                + expense.name()
                + "” and all of its recorded payment history and reminders?\n\n"
                + "Tip: Edit the expense and change its status to Canceled if you want to keep"
                + " past payment records.",
            ButtonType.CANCEL,
            ButtonType.OK);
    confirm.initOwner(owner);
    confirm.initModality(Modality.WINDOW_MODAL);
    confirm.setTitle("Delete expense");
    confirm.setHeaderText("Permanently delete “" + expense.name() + "”?");
    applyDialogTheme(confirm.getDialogPane(), darkTheme);
    Node okButton = confirm.getDialogPane().lookupButton(ButtonType.OK);
    if (okButton instanceof Button b) {
      b.setText("Delete permanently");
      b.getStyleClass().addAll("app-button", "danger");
    }
    return confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
  }

  static void showError(Stage owner, boolean darkTheme, String text) {
    Alert alert = new Alert(Alert.AlertType.ERROR, text, ButtonType.OK);
    alert.initOwner(owner);
    alert.initModality(Modality.WINDOW_MODAL);
    alert.setTitle("BillVault Desktop");
    alert.setHeaderText("BillVault could not complete that action");
    applyDialogTheme(alert.getDialogPane(), darkTheme);
    alert.showAndWait();
  }

  static String formatErrorMessage(Throwable ex) {
    if (ex instanceof ResponseStatusException r && r.getReason() != null) {
      return r.getReason();
    }
    if (ex instanceof ConstraintViolationException v) {
      return String.join(
          "\n",
          v.getConstraintViolations().stream()
              .map(c -> c.getPropertyPath() + ": " + c.getMessage())
              .sorted()
              .toList());
    }
    if (ex instanceof DataIntegrityViolationException) {
      return "This conflicts with an existing record. Category names must be unique.";
    }
    return ex.getMessage() == null || ex.getMessage().isBlank()
        ? ex.getClass().getSimpleName()
        : ex.getMessage();
  }

  private static Dialog<ButtonType> baseDialog(Stage owner, boolean darkTheme, String title) {
    Dialog<ButtonType> d = new Dialog<>();
    d.initOwner(owner);
    d.initModality(Modality.WINDOW_MODAL);
    d.setTitle(title);
    d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
    applyDialogTheme(d.getDialogPane(), darkTheme);
    Node ok = d.getDialogPane().lookupButton(ButtonType.OK);
    if (ok instanceof Button b) {
      b.setText("Save");
      b.getStyleClass().addAll("app-button", "primary");
    }
    Node cancel = d.getDialogPane().lookupButton(ButtonType.CANCEL);
    if (cancel instanceof Button b) {
      b.getStyleClass().addAll("app-button", "secondary");
    }
    return d;
  }

  private static void applyDialogTheme(javafx.scene.control.DialogPane pane, boolean darkTheme) {
    String css =
        Objects.requireNonNull(DesktopDialogs.class.getResource("desktop.css")).toExternalForm();
    if (!pane.getStylesheets().contains(css)) {
      pane.getStylesheets().add(css);
    }
    pane.getStyleClass().add("styled-dialog");
    if (darkTheme) {
      if (!pane.getStyleClass().contains("dark")) {
        pane.getStyleClass().add("dark");
      }
    } else {
      pane.getStyleClass().remove("dark");
    }
  }

  private static Label inlineErrorBanner() {
    Label banner = new Label();
    banner.getStyleClass().add("error-banner");
    banner.setWrapText(true);
    banner.setMaxWidth(Double.MAX_VALUE);
    banner.setVisible(false);
    banner.setManaged(false);
    return banner;
  }

  private static void onSave(Dialog<ButtonType> dialog, Label errorBanner, Runnable saveAction) {
    dialog
        .getDialogPane()
        .lookupButton(ButtonType.OK)
        .addEventFilter(
            ActionEvent.ACTION,
            event -> {
              try {
                errorBanner.setVisible(false);
                errorBanner.setManaged(false);
                saveAction.run();
              } catch (Exception ex) {
                event.consume();
                errorBanner.setText(formatErrorMessage(ex));
                errorBanner.setVisible(true);
                errorBanner.setManaged(true);
              }
            });
  }

  private static GridPane twoColumnGrid() {
    GridPane grid = new GridPane();
    grid.setHgap(14);
    grid.setVgap(12);
    grid.setPadding(new Insets(6));
    ColumnConstraints col1 = new ColumnConstraints();
    col1.setPercentWidth(50);
    col1.setHgrow(Priority.ALWAYS);
    ColumnConstraints col2 = new ColumnConstraints();
    col2.setPercentWidth(50);
    col2.setHgrow(Priority.ALWAYS);
    grid.getColumnConstraints().addAll(col1, col2);
    return grid;
  }

  private static VBox fieldBlock(String labelText, Control control) {
    Label label = new Label(labelText);
    label.getStyleClass().add("field-label");
    label.setLabelFor(control);
    control.setMaxWidth(Double.MAX_VALUE);
    VBox box = new VBox(5, label, control);
    GridPane.setHgrow(box, Priority.ALWAYS);
    return box;
  }

  private static <E extends Enum<E>> ComboBox<E> enumCombo(E[] values, E selected) {
    ComboBox<E> combo = new ComboBox<>(FXCollections.observableArrayList(values));
    combo.setMaxWidth(Double.MAX_VALUE);
    combo.setValue(selected);
    combo.setConverter(
        new StringConverter<>() {
          @Override
          public String toString(E object) {
            return DesktopFormat.titleCase(object);
          }

          @Override
          public E fromString(String string) {
            return null;
          }
        });
    return combo;
  }
}
