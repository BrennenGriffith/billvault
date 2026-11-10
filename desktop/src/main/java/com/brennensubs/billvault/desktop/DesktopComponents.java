package com.brennensubs.billvault.desktop;

import com.brennensubs.billvault.dto.Api.ExpenseView;
import com.brennensubs.billvault.dto.Api.Trend;
import com.brennensubs.billvault.model.ExpenseStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Reusable JavaFX component factories and custom table cell renderers. */
final class DesktopComponents {
  private DesktopComponents() {}

  static Button button(String text, String styleClass, String iconPath, Runnable action) {
    Button b = new Button(text);
    b.getStyleClass().add("app-button");
    if (styleClass != null && !styleClass.isBlank()) {
      b.getStyleClass().add(styleClass);
    }
    if (iconPath != null) {
      b.setGraphic(DesktopIcons.icon(iconPath, 15));
      b.setGraphicTextGap(7);
    }
    b.setOnAction(e -> action.run());
    return b;
  }

  static Button primaryButton(String text, String iconPath, Runnable action) {
    return button(text, "primary", iconPath, action);
  }

  static Button secondaryButton(String text, String iconPath, Runnable action) {
    return button(text, "secondary", iconPath, action);
  }

  static Button dangerButton(String text, String iconPath, Runnable action) {
    return button(text, "danger", iconPath, action);
  }

  static Button subtleButton(String text, String iconPath, Runnable action) {
    return button(text, "subtle", iconPath, action);
  }

  static Button iconButton(String accessibleName, String iconPath, Runnable action) {
    Button b = new Button();
    b.getStyleClass().addAll("app-button", "icon-button");
    b.setGraphic(DesktopIcons.icon(iconPath, 17));
    b.setAccessibleText(accessibleName);
    b.setTooltip(new Tooltip(accessibleName));
    b.setOnAction(e -> action.run());
    return b;
  }

  static VBox statCard(
      String title, String value, String note, String iconPath, boolean featured) {
    Label titleLabel = new Label(title);
    titleLabel.getStyleClass().add("stat-title");
    titleLabel.setWrapText(true);

    StackPane iconBadge = new StackPane(DesktopIcons.icon(iconPath, 17));
    iconBadge.getStyleClass().add("stat-icon");

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    HBox top = new HBox(8, titleLabel, spacer, iconBadge);
    top.setAlignment(Pos.CENTER_LEFT);

    Label valueLabel = new Label(value);
    valueLabel.getStyleClass().add("stat-value");
    valueLabel.setWrapText(true);

    Region dot = new Region();
    dot.getStyleClass().add("tiny-dot");
    Label noteLabel = new Label(note);
    noteLabel.getStyleClass().add("stat-note");
    noteLabel.setWrapText(true);
    HBox bottom = new HBox(6, dot, noteLabel);
    bottom.setAlignment(Pos.CENTER_LEFT);

    VBox card = new VBox(10, top, valueLabel, bottom);
    card.getStyleClass().add("stat-card");
    if (featured) {
      card.getStyleClass().add("featured");
    }
    card.setMinWidth(160);
    card.setMaxWidth(Double.MAX_VALUE);
    HBox.setHgrow(card, Priority.ALWAYS);
    return card;
  }

  static VBox panel(String title, String subtitle, Node rightControl, Node content) {
    Label heading = new Label(title);
    heading.getStyleClass().add("panel-title");
    VBox titleBox = new VBox(3, heading);
    if (subtitle != null && !subtitle.isBlank()) {
      Label sub = new Label(subtitle);
      sub.getStyleClass().add("panel-subtitle");
      sub.setWrapText(true);
      titleBox.getChildren().add(sub);
    }
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    HBox header = new HBox(12, titleBox, spacer);
    header.setAlignment(Pos.CENTER_LEFT);
    header.getStyleClass().add("panel-header");
    if (rightControl != null) {
      header.getChildren().add(rightControl);
    }

    VBox card = new VBox(0, header, content);
    card.getStyleClass().add("panel");
    card.setMinWidth(0);
    card.setMaxWidth(Double.MAX_VALUE);
    return card;
  }

  static Label tag(String text) {
    Label l = new Label(text);
    l.getStyleClass().add("tag-pill");
    return l;
  }

  static Label statusBadge(ExpenseStatus status) {
    String label = DesktopFormat.titleCase(status);
    Label badge = new Label(label);
    badge.getStyleClass().add("status-badge");
    if (status != null) {
      badge.getStyleClass().add("status-" + status.name().toLowerCase(Locale.US));
    }
    return badge;
  }

  static HBox categoryChip(String name, String hexColor) {
    Region dot = new Region();
    dot.getStyleClass().add("category-dot");
    String safe = DesktopFormat.safeHexColor(hexColor, "#6056dc");
    dot.setStyle("-fx-background-color: " + safe + ";");
    Label text = new Label(name == null ? "Uncategorized" : name);
    text.getStyleClass().add("cell-secondary");
    HBox box = new HBox(8, dot, text);
    box.setAlignment(Pos.CENTER_LEFT);
    return box;
  }

  static StackPane brandTile(String name, String hexColor) {
    String safe = DesktopFormat.safeHexColor(hexColor, "#6056dc");
    Label initials = new Label(DesktopFormat.initials(name));
    initials.getStyleClass().add("brand-tile-text");
    initials.setStyle("-fx-text-fill: " + safe + ";");
    StackPane tile = new StackPane(initials);
    tile.getStyleClass().add("brand-tile");
    tile.setStyle("-fx-background-color: " + safe + "22;");
    return tile;
  }

  static VBox emptyState(
      String iconPath, String title, String subtitle, String actionLabel, Runnable action) {
    StackPane iconWrap = new StackPane(DesktopIcons.icon(iconPath, 24));
    iconWrap.getStyleClass().add("empty-icon");

    Label heading = new Label(title);
    heading.getStyleClass().add("empty-title");

    Label body = new Label(subtitle);
    body.getStyleClass().add("empty-subtitle");
    body.setWrapText(true);

    VBox box = new VBox(8, iconWrap, heading, body);
    box.getStyleClass().add("empty-state");
    box.setAlignment(Pos.CENTER);
    if (actionLabel != null && action != null) {
      Button cta = primaryButton(actionLabel, DesktopIcons.PLUS, action);
      VBox.setMargin(cta, new Insets(6, 0, 0, 0));
      box.getChildren().add(cta);
    }
    return box;
  }

  static Node trendChart(List<Trend> trends, String currency) {
    List<Trend> filtered =
        trends == null
            ? List.of()
            : trends.stream().filter(t -> currency.equals(t.currency())).toList();
    BigDecimal max =
        filtered.stream()
            .map(Trend::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::max);
    if (filtered.isEmpty() || max.compareTo(BigDecimal.ZERO) <= 0) {
      return emptyState(
          DesktopIcons.REPORTS,
          "No recorded payments yet",
          "Record a payment on an active bill or subscription to see your 6-month trend.",
          null,
          null);
    }
    HBox chart = new HBox(14);
    chart.getStyleClass().add("trend-chart");
    chart.setAlignment(Pos.BOTTOM_CENTER);

    for (int i = 0; i < filtered.size(); i++) {
      Trend t = filtered.get(i);
      boolean latest = i == filtered.size() - 1;

      Label amountLabel =
          new Label(
              t.amount().compareTo(BigDecimal.ZERO) > 0
                  ? t.amount().setScale(0, RoundingMode.HALF_UP).toPlainString()
                  : "0");
      amountLabel.getStyleClass().add("trend-amount");

      double ratio =
          max.compareTo(BigDecimal.ZERO) > 0
              ? t.amount().divide(max, 4, RoundingMode.HALF_UP).doubleValue()
              : 0.0;
      double barHeight = Math.max(6.0, Math.round(ratio * 110.0));

      Region bar = new Region();
      bar.getStyleClass().add("trend-bar");
      if (latest) {
        bar.getStyleClass().add("trend-bar-latest");
      }
      bar.setMinHeight(barHeight);
      bar.setPrefHeight(barHeight);
      bar.setMaxHeight(barHeight);
      bar.setMaxWidth(38);
      Tooltip.install(
          bar, new Tooltip(t.month() + " · " + DesktopFormat.money(t.currency(), t.amount())));

      StackPane track = new StackPane(bar);
      track.getStyleClass().add("trend-bar-track");
      track.setAlignment(Pos.BOTTOM_CENTER);
      VBox.setVgrow(track, Priority.ALWAYS);

      Label monthLabel = new Label(DesktopFormat.shortMonthLabel(t.month()));
      monthLabel.getStyleClass().add("trend-month");

      VBox col = new VBox(6, amountLabel, track, monthLabel);
      col.setAlignment(Pos.BOTTOM_CENTER);
      HBox.setHgrow(col, Priority.ALWAYS);
      chart.getChildren().add(col);
    }
    return chart;
  }

  static void configureExpenseTable(
      TableView<ExpenseView> table,
      boolean compact,
      Consumer<ExpenseView> onEdit,
      Consumer<ExpenseView> onPay,
      Consumer<ExpenseView> onReminder,
      Consumer<ExpenseView> onDelete,
      Runnable onAdd) {
    table.getColumns().clear();
    table.getStyleClass().add("modern-table");
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    table.setPlaceholder(
        emptyState(
            DesktopIcons.WALLET,
            "No matching expenses",
            "Add a recurring subscription or household bill to track due dates and payments.",
            onAdd != null ? "Add expense" : null,
            onAdd));

    TableColumn<ExpenseView, ExpenseView> nameCol = new TableColumn<>("EXPENSE");
    nameCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
    nameCol.setPrefWidth(230);
    nameCol.setCellFactory(
        col ->
            new TableCell<>() {
              @Override
              protected void updateItem(ExpenseView item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                  setGraphic(null);
                  setText(null);
                  return;
                }
                StackPane tile = brandTile(item.name(), item.categoryColor());
                Label primary = new Label(item.name());
                primary.getStyleClass().add("cell-primary");
                VBox text = new VBox(2, primary);
                String subText =
                    item.paymentMethod() != null && !item.paymentMethod().isBlank()
                        ? item.paymentMethod()
                        : (item.description() != null && !item.description().isBlank()
                            ? item.description()
                            : DesktopFormat.titleCase(item.kind()));
                if (subText != null && !subText.isBlank()) {
                  Label sub = new Label(subText);
                  sub.getStyleClass().add("cell-muted");
                  text.getChildren().add(sub);
                }
                HBox row = new HBox(10, tile, text);
                row.setAlignment(Pos.CENTER_LEFT);
                setGraphic(row);
                setText(null);
              }
            });
    table.getColumns().add(nameCol);

    TableColumn<ExpenseView, ExpenseView> categoryCol = new TableColumn<>("CATEGORY");
    categoryCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
    categoryCol.setPrefWidth(145);
    categoryCol.setCellFactory(
        col ->
            new TableCell<>() {
              @Override
              protected void updateItem(ExpenseView item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                  setGraphic(null);
                  setText(null);
                } else {
                  setGraphic(categoryChip(item.categoryName(), item.categoryColor()));
                  setText(null);
                }
              }
            });
    table.getColumns().add(categoryCol);

    TableColumn<ExpenseView, ExpenseView> amountCol = new TableColumn<>("AMOUNT");
    amountCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
    amountCol.setPrefWidth(135);
    amountCol.setCellFactory(
        col ->
            new TableCell<>() {
              @Override
              protected void updateItem(ExpenseView item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                  setGraphic(null);
                  setText(null);
                  return;
                }
                Label amt = new Label(DesktopFormat.money(item.currency(), item.amount()));
                amt.getStyleClass().add("cell-primary");
                if (item.variableAmount()) {
                  Label varTag = new Label("Variable");
                  varTag.getStyleClass().add("cell-muted");
                  VBox box = new VBox(1, amt, varTag);
                  box.setAlignment(Pos.CENTER_LEFT);
                  setGraphic(box);
                } else {
                  setGraphic(amt);
                }
                setText(null);
              }
            });
    table.getColumns().add(amountCol);

    if (!compact) {
      TableColumn<ExpenseView, String> freqCol = new TableColumn<>("CYCLE");
      freqCol.setCellValueFactory(
          d -> new SimpleStringProperty(DesktopFormat.frequencyLabel(d.getValue())));
      freqCol.setPrefWidth(115);
      table.getColumns().add(freqCol);
    }

    TableColumn<ExpenseView, ExpenseView> dueCol = new TableColumn<>("NEXT DUE");
    dueCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
    dueCol.setPrefWidth(125);
    dueCol.setCellFactory(
        col ->
            new TableCell<>() {
              @Override
              protected void updateItem(ExpenseView item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                  setGraphic(null);
                  setText(null);
                  return;
                }
                Label due = new Label(DesktopFormat.dateLabel(item.nextDueDate()));
                boolean overdue =
                    item.status() == ExpenseStatus.ACTIVE
                        && item.nextDueDate() != null
                        && item.nextDueDate().isBefore(LocalDate.now());
                due.getStyleClass().add(overdue ? "cell-overdue" : "cell-secondary");
                setGraphic(due);
                setText(null);
              }
            });
    table.getColumns().add(dueCol);

    if (!compact) {
      TableColumn<ExpenseView, ExpenseStatus> statusCol = new TableColumn<>("STATUS");
      statusCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().status()));
      statusCol.setPrefWidth(105);
      statusCol.setCellFactory(
          col ->
              new TableCell<>() {
                @Override
                protected void updateItem(ExpenseStatus item, boolean empty) {
                  super.updateItem(item, empty);
                  if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                  } else {
                    setGraphic(statusBadge(item));
                    setText(null);
                  }
                }
              });
      table.getColumns().add(statusCol);
    }

    TableColumn<ExpenseView, ExpenseView> actionsCol = new TableColumn<>("ACTIONS");
    actionsCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
    actionsCol.setPrefWidth(compact ? 115 : 150);
    actionsCol.setCellFactory(
        col ->
            new TableCell<>() {
              @Override
              protected void updateItem(ExpenseView item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                  setGraphic(null);
                  setText(null);
                  return;
                }
                HBox actions = new HBox(6);
                actions.setAlignment(Pos.CENTER_RIGHT);
                if (item.status() == ExpenseStatus.ACTIVE && onPay != null) {
                  Button payBtn =
                      subtleButton("Pay", DesktopIcons.CHECK, () -> onPay.accept(item));
                  payBtn.getStyleClass().add("row-action-primary");
                  actions.getChildren().add(payBtn);
                }
                if (onEdit != null) {
                  Button editBtn =
                      iconButton("Edit " + item.name(), DesktopIcons.EDIT, () -> onEdit.accept(item));
                  actions.getChildren().add(editBtn);
                }
                setGraphic(actions);
                setText(null);
              }
            });
    table.getColumns().add(actionsCol);

    table.setRowFactory(
        tv -> {
          TableRow<ExpenseView> row = new TableRow<>();
          row.setOnMouseClicked(
              e -> {
                if (e.getClickCount() == 2 && !row.isEmpty() && onEdit != null) {
                  onEdit.accept(row.getItem());
                }
              });
          ContextMenu menu = new ContextMenu();
          MenuItem payItem = new MenuItem("Record payment");
          payItem.setOnAction(
              e -> {
                if (!row.isEmpty() && onPay != null) onPay.accept(row.getItem());
              });
          MenuItem editItem = new MenuItem("Edit expense");
          editItem.setOnAction(
              e -> {
                if (!row.isEmpty() && onEdit != null) onEdit.accept(row.getItem());
              });
          MenuItem remItem = new MenuItem("Reminder settings");
          remItem.setOnAction(
              e -> {
                if (!row.isEmpty() && onReminder != null) onReminder.accept(row.getItem());
              });
          MenuItem delItem = new MenuItem("Delete permanently");
          delItem.setOnAction(
              e -> {
                if (!row.isEmpty() && onDelete != null) onDelete.accept(row.getItem());
              });
          menu.getItems().addAll(payItem, editItem, remItem, delItem);
          row.emptyProperty()
              .addListener((obs, wasEmpty, isEmpty) -> row.setContextMenu(isEmpty ? null : menu));
          return row;
        });
  }

  static <T> TableColumn<T, String> textColumn(
      String title, Function<T, String> extractor, double prefWidth, String styleClass) {
    TableColumn<T, String> col = new TableColumn<>(title);
    col.setCellValueFactory(d -> new SimpleStringProperty(extractor.apply(d.getValue())));
    col.setPrefWidth(prefWidth);
    if (styleClass != null) {
      col.setCellFactory(
          c ->
              new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                  super.updateItem(item, empty);
                  if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                  } else {
                    Label l = new Label(item);
                    l.getStyleClass().add(styleClass);
                    setGraphic(l);
                    setText(null);
                  }
                }
              });
    }
    return col;
  }
}
