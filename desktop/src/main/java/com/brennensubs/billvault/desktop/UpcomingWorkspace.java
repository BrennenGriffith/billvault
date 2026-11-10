package com.brennensubs.billvault.desktop;

import com.brennensubs.billvault.dto.Api.ExpenseView;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Projected monthly payment calendar and overdue expense review workspace. */
final class UpcomingWorkspace {
  private final ScrollPane scrollPane;
  private final VBox content = new VBox(18);
  private YearMonth selectedMonth = YearMonth.now();
  private final Consumer<YearMonth> onMonthChange;
  private final Consumer<ExpenseView> onEdit;
  private final Consumer<ExpenseView> onPay;
  private final Runnable onAdd;

  UpcomingWorkspace(
      Consumer<YearMonth> onMonthChange,
      Consumer<ExpenseView> onEdit,
      Consumer<ExpenseView> onPay,
      Runnable onAdd) {
    this.onMonthChange = onMonthChange;
    this.onEdit = onEdit;
    this.onPay = onPay;
    this.onAdd = onAdd;

    content.getStyleClass().add("workspace-body");
    content.setMinWidth(0);

    scrollPane = new ScrollPane(content);
    scrollPane.setFitToWidth(true);
    scrollPane.setMinSize(0, 0);
    scrollPane.getStyleClass().add("workspace-scroll");
  }

  Node node() {
    return scrollPane;
  }

  YearMonth selectedMonth() {
    return selectedMonth;
  }

  void update(List<ExpenseView> overdue, List<Map<String, Object>> calendarItems) {
    content.getChildren().clear();

    if (overdue != null && !overdue.isEmpty()) {
      TableView<ExpenseView> overdueTable = new TableView<>();
      DesktopComponents.configureExpenseTable(
          overdueTable, true, onEdit, onPay, null, null, onAdd);
      overdueTable.getItems().setAll(overdue);
      overdueTable.setPrefHeight(Math.min(260, 56 + overdue.size() * 48.0));
      overdueTable.setMinHeight(140);

      VBox overduePanel =
          DesktopComponents.panel(
              "Overdue now (" + overdue.size() + ")",
              "Active expenses whose due date has passed without a recorded payment",
              DesktopComponents.tag("Action needed"),
              overdueTable);
      content.getChildren().add(overduePanel);
    }

    Button prevMonth =
        DesktopComponents.iconButton(
            "Previous month",
            DesktopIcons.CHEVRON_LEFT,
            () -> {
              selectedMonth = selectedMonth.minusMonths(1);
              onMonthChange.accept(selectedMonth);
            });
    Label monthLabel = new Label(DesktopFormat.monthLabel(selectedMonth));
    monthLabel.getStyleClass().add("month-picker-label");
    Button nextMonth =
        DesktopComponents.iconButton(
            "Next month",
            DesktopIcons.CHEVRON_RIGHT,
            () -> {
              selectedMonth = selectedMonth.plusMonths(1);
              onMonthChange.accept(selectedMonth);
            });
    Button todayBtn =
        DesktopComponents.subtleButton(
            "This month",
            null,
            () -> {
              selectedMonth = YearMonth.now();
              onMonthChange.accept(selectedMonth);
            });
    HBox monthControls = new HBox(8, prevMonth, monthLabel, nextMonth, todayBtn);
    monthControls.setAlignment(Pos.CENTER_RIGHT);

    Node calendarBody;
    if (calendarItems == null || calendarItems.isEmpty()) {
      calendarBody =
          DesktopComponents.emptyState(
              DesktopIcons.CALENDAR,
              "Nothing due in " + DesktopFormat.monthLabel(selectedMonth),
              "Projected occurrences for active bills and subscriptions will appear here.",
              "Add expense",
              onAdd);
    } else {
      VBox list = new VBox(0);
      list.setPadding(new Insets(4, 20, 16, 20));
      for (Map<String, Object> item : calendarItems) {
        LocalDate date = (LocalDate) item.get("date");
        String name = String.valueOf(item.get("name"));
        BigDecimal amount = (BigDecimal) item.get("amount");
        String currency = String.valueOf(item.get("currency"));

        Label dayNum =
            new Label(date == null ? "--" : String.format("%02d", date.getDayOfMonth()));
        dayNum.getStyleClass().add("calendar-day-num");
        Label dayWeek = new Label(DesktopFormat.weekdayShort(date));
        dayWeek.getStyleClass().add("calendar-day-week");
        VBox dateBadge = new VBox(1, dayNum, dayWeek);
        dateBadge.setAlignment(Pos.CENTER);
        dateBadge.getStyleClass().add("calendar-date-badge");

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("cell-primary");
        Label fullDate = new Label(DesktopFormat.dateLabel(date));
        fullDate.getStyleClass().add("cell-muted");
        VBox titleBox = new VBox(2, nameLabel, fullDate);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Label amountLabel = new Label(DesktopFormat.money(currency, amount));
        amountLabel.getStyleClass().add("cell-primary");

        Label projectedTag = DesktopComponents.tag("Projected");

        HBox row = new HBox(16, dateBadge, titleBox, amountLabel, projectedTag);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("calendar-row");
        list.getChildren().add(row);
      }
      calendarBody = list;
    }

    VBox calendarPanel =
        DesktopComponents.panel(
            "Payment calendar",
            "Estimated occurrences for active expenses, including future repeats",
            monthControls,
            calendarBody);
    content.getChildren().add(calendarPanel);
  }
}
