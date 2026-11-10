package com.brennensubs.billvault.desktop;

import com.brennensubs.billvault.dto.Api.NotificationView;
import java.util.List;
import java.util.function.Consumer;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** In-app reminder notifications workspace. */
final class RemindersWorkspace {
  private final VBox root = new VBox(14);
  private final TableView<NotificationView> table = new TableView<>();
  private final Label unreadTag = DesktopComponents.tag("0 unread");

  RemindersWorkspace(
      Runnable onRefreshReminders,
      Consumer<NotificationView> onMarkRead,
      Consumer<String> onRequireSelectionError) {
    root.getStyleClass().add("workspace-body");
    root.setMinSize(0, 0);

    table.getStyleClass().add("modern-table");
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    table.setPlaceholder(
        DesktopComponents.emptyState(
            DesktopIcons.BELL,
            "You're all caught up",
            "Reminders are checked automatically while BillVault Desktop is open.",
            null,
            null));

    TableColumn<NotificationView, NotificationView> msgCol = new TableColumn<>("REMINDER");
    msgCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
    msgCol.setPrefWidth(320);
    msgCol.setCellFactory(
        col ->
            new TableCell<>() {
              @Override
              protected void updateItem(NotificationView item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                  setGraphic(null);
                  setText(null);
                  return;
                }
                Region dot = new Region();
                dot.getStyleClass().add(item.read() ? "read-dot" : "unread-dot");
                Label text = new Label(item.message());
                text.getStyleClass().add(item.read() ? "cell-muted" : "cell-primary");
                HBox box = new HBox(10, dot, text);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
                setText(null);
              }
            });
    table.getColumns().add(msgCol);

    table
        .getColumns()
        .add(
            DesktopComponents.textColumn(
                "DUE OCCURRENCE",
                n -> DesktopFormat.dateLabel(n.occurrenceDate()),
                150,
                "cell-secondary"));

    TableColumn<NotificationView, NotificationView> statusCol = new TableColumn<>("STATE");
    statusCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
    statusCol.setPrefWidth(130);
    statusCol.setCellFactory(
        col ->
            new TableCell<>() {
              @Override
              protected void updateItem(NotificationView item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                  setGraphic(null);
                  setText(null);
                  return;
                }
                Label badge = new Label(item.read() ? "Read" : "Unread");
                badge
                    .getStyleClass()
                    .addAll(
                        "status-badge",
                        item.read() ? "status-canceled" : "status-active");
                setGraphic(badge);
                setText(null);
              }
            });
    table.getColumns().add(statusCol);

    TableColumn<NotificationView, NotificationView> actionCol = new TableColumn<>("ACTION");
    actionCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
    actionCol.setPrefWidth(130);
    actionCol.setCellFactory(
        col ->
            new TableCell<>() {
              @Override
              protected void updateItem(NotificationView item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.read()) {
                  setGraphic(null);
                  setText(null);
                  return;
                }
                Button markBtn =
                    DesktopComponents.subtleButton(
                        "Mark read", DesktopIcons.CHECK, () -> onMarkRead.accept(item));
                setGraphic(markBtn);
                setText(null);
              }
            });
    table.getColumns().add(actionCol);

    table.setMinHeight(220);
    VBox.setVgrow(table, Priority.ALWAYS);

    FlowPane toolbar =
        new FlowPane(
            10,
            10,
            DesktopComponents.secondaryButton(
                "Refresh reminders", DesktopIcons.REFRESH, onRefreshReminders),
            DesktopComponents.primaryButton(
                "Mark selected read",
                DesktopIcons.CHECK,
                () -> {
                  NotificationView selected = table.getSelectionModel().getSelectedItem();
                  if (selected == null) {
                    onRequireSelectionError.accept("Select a reminder in the list first.");
                  } else {
                    onMarkRead.accept(selected);
                  }
                }));
    toolbar.getStyleClass().add("table-footer");
    toolbar.setAlignment(Pos.CENTER_LEFT);

    VBox body = new VBox(0, table, toolbar);
    VBox.setVgrow(table, Priority.ALWAYS);

    VBox panel =
        DesktopComponents.panel(
            "In-app reminders",
            "Generated locally while BillVault Desktop is open based on each expense's lead days",
            unreadTag,
            body);
    VBox.setVgrow(panel, Priority.ALWAYS);
    root.getChildren().add(panel);
  }

  Node node() {
    return root;
  }

  void update(List<NotificationView> notices) {
    List<NotificationView> safe = notices == null ? List.of() : notices;
    table.getItems().setAll(safe);
    long unread = safe.stream().filter(n -> !n.read()).count();
    unreadTag.setText(unread + " unread");
  }
}
