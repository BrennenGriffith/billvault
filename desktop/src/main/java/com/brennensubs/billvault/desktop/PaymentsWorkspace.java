package com.brennensubs.billvault.desktop;

import com.brennensubs.billvault.dto.Api.PaymentView;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Recorded payment history ledger workspace. */
final class PaymentsWorkspace {
  private final VBox root = new VBox(14);
  private final TableView<PaymentView> table = new TableView<>();
  private final Label countTag = DesktopComponents.tag("0 payments");

  PaymentsWorkspace() {
    root.getStyleClass().add("workspace-body");
    root.setMinSize(0, 0);

    table.getStyleClass().add("modern-table");
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    table.setPlaceholder(
        DesktopComponents.emptyState(
            DesktopIcons.HISTORY,
            "Your payment story starts here",
            "When you record a payment on an expense, the actual amount and date appear in this"
                + " ledger.",
            null,
            null));

    table
        .getColumns()
        .add(
            DesktopComponents.textColumn(
                "EXPENSE", PaymentView::expenseName, 220, "cell-primary"));
    table
        .getColumns()
        .add(
            DesktopComponents.textColumn(
                "PAID ON",
                p -> DesktopFormat.dateLabel(p.paymentDate()),
                135,
                "cell-secondary"));
    table
        .getColumns()
        .add(
            DesktopComponents.textColumn(
                "OCCURRENCE",
                p -> DesktopFormat.dateLabel(p.occurrenceDate()),
                135,
                "cell-secondary"));
    table
        .getColumns()
        .add(
            DesktopComponents.textColumn(
                "AMOUNT",
                p -> DesktopFormat.money(p.currency(), p.amount()),
                145,
                "cell-primary"));
    table
        .getColumns()
        .add(
            DesktopComponents.textColumn(
                "NOTES",
                p -> p.notes() == null || p.notes().isBlank() ? "—" : p.notes(),
                220,
                "cell-muted"));

    table.setMinHeight(220);
    VBox.setVgrow(table, Priority.ALWAYS);

    VBox panel =
        DesktopComponents.panel(
            "Recorded payments",
            "Actual amounts paid, kept separate from your recurring estimates",
            countTag,
            table);
    VBox.setVgrow(panel, Priority.ALWAYS);
    root.getChildren().add(panel);
  }

  Node node() {
    return root;
  }

  void update(List<PaymentView> payments) {
    List<PaymentView> safe = payments == null ? List.of() : payments;
    table.getItems().setAll(safe);
    countTag.setText(safe.size() + (safe.size() == 1 ? " payment" : " payments"));
  }
}
