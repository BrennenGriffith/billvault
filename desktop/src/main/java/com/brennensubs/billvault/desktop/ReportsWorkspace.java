package com.brennensubs.billvault.desktop;

import com.brennensubs.billvault.dto.Api.CategoryTotal;
import com.brennensubs.billvault.dto.Api.CategoryView;
import com.brennensubs.billvault.dto.Api.Dashboard;
import com.brennensubs.billvault.dto.Api.MoneyTotal;
import com.brennensubs.billvault.dto.Api.ReportTotal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Financial reports, category breakdown, 6-month actual trends, and CSV export workspace. */
final class ReportsWorkspace {
  private final ScrollPane scrollPane;
  private final VBox content = new VBox(18);
  private final DatePicker reportDate = new DatePicker(LocalDate.now());
  private final DatePicker fromDate = new DatePicker(LocalDate.now().withDayOfYear(1));
  private final DatePicker toDate = new DatePicker(LocalDate.now());
  private final Consumer<LocalDate> onRequestReport;
  private final BiConsumer<LocalDate, LocalDate> onExportCsv;

  ReportsWorkspace(
      Consumer<LocalDate> onRequestReport,
      BiConsumer<LocalDate, LocalDate> onExportCsv) {
    this.onRequestReport = onRequestReport;
    this.onExportCsv = onExportCsv;

    content.getStyleClass().add("workspace-body");
    content.setMinWidth(0);

    reportDate.setPrefWidth(145);
    fromDate.setPrefWidth(145);
    toDate.setPrefWidth(145);

    scrollPane = new ScrollPane(content);
    scrollPane.setFitToWidth(true);
    scrollPane.setMinSize(0, 0);
    scrollPane.getStyleClass().add("workspace-scroll");
  }

  Node node() {
    return scrollPane;
  }

  LocalDate selectedReportDate() {
    return reportDate.getValue() == null ? LocalDate.now() : reportDate.getValue();
  }

  void update(
      Dashboard dashboard,
      List<CategoryView> categories,
      List<ReportTotal> reportTotals,
      String activeCurrency) {
    content.getChildren().clear();
    String currency =
        activeCurrency == null || activeCurrency.isBlank() ? "USD" : activeCurrency;
    LocalDate selected = selectedReportDate();
    YearMonth ym = YearMonth.from(selected);

    // Top report controls & export bar
    Label monthLabel = new Label("Report month:");
    monthLabel.getStyleClass().add("field-label");
    FlowPane reportControls =
        new FlowPane(
            10,
            10,
            monthLabel,
            reportDate,
            DesktopComponents.primaryButton(
                "Show totals",
                DesktopIcons.REPORTS,
                () -> onRequestReport.accept(reportDate.getValue())),
            DesktopComponents.secondaryButton(
                "Export month CSV",
                DesktopIcons.DOWNLOAD,
                () -> onExportCsv.accept(ym.atDay(1), ym.atEndOfMonth())),
            DesktopComponents.secondaryButton(
                "Export year CSV",
                DesktopIcons.DOWNLOAD,
                () ->
                    onExportCsv.accept(
                        LocalDate.of(ym.getYear(), 1, 1),
                        LocalDate.of(ym.getYear(), 12, 31))));
    reportControls.setAlignment(Pos.CENTER_LEFT);
    reportControls.getStyleClass().add("filter-bar");

    VBox controlsPanel =
        DesktopComponents.panel(
            "Report period & quick export",
            "Inspect actual recorded payments for any month and year, separated by currency",
            DesktopComponents.tag(DesktopFormat.monthLabel(ym)),
            reportControls);
    content.getChildren().add(controlsPanel);

    // 3 KPI Stat Cards for the active currency
    ReportTotal currReport =
        reportTotals == null
            ? new ReportTotal(currency, BigDecimal.ZERO, BigDecimal.ZERO)
            : reportTotals.stream()
                .filter(r -> currency.equals(r.currency()))
                .findFirst()
                .orElse(new ReportTotal(currency, BigDecimal.ZERO, BigDecimal.ZERO));

    MoneyTotal currProjection =
        dashboard == null
            ? new MoneyTotal(currency, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
            : dashboard.totals().stream()
                .filter(t -> currency.equals(t.currency()))
                .findFirst()
                .orElse(
                    new MoneyTotal(
                        currency, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

    GridPane statGrid = new GridPane();
    statGrid.setHgap(14);
    statGrid.setVgap(14);
    for (int i = 0; i < 3; i++) {
      ColumnConstraints cc = new ColumnConstraints();
      cc.setPercentWidth(33.33);
      cc.setHgrow(Priority.ALWAYS);
      statGrid.getColumnConstraints().add(cc);
    }
    statGrid.add(
        DesktopComponents.statCard(
            "Recorded in selected month",
            DesktopFormat.money(currency, currReport.paidInMonth()),
            DesktopFormat.monthLabel(ym),
            DesktopIcons.HISTORY,
            true),
        0,
        0);
    statGrid.add(
        DesktopComponents.statCard(
            "Recorded in selected year",
            DesktopFormat.money(currency, currReport.paidInYear()),
            String.valueOf(ym.getYear()),
            DesktopIcons.HISTORY,
            false),
        1,
        0);
    statGrid.add(
        DesktopComponents.statCard(
            "Annual recurring estimate",
            DesktopFormat.money(currency, currProjection.annual()),
            "Current active expenses, annualized",
            DesktopIcons.SUBSCRIPTIONS,
            false),
        2,
        0);
    content.getChildren().add(statGrid);

    // Multi-currency summary if records exist
    if (reportTotals != null && !reportTotals.isEmpty()) {
      VBox ledgerRows = new VBox(8);
      ledgerRows.setPadding(new Insets(14, 20, 18, 20));
      for (ReportTotal row : reportTotals) {
        Label curBadge = DesktopComponents.tag(row.currency());
        Label mText =
            new Label(
                "Month ("
                    + DesktopFormat.monthLabel(ym)
                    + "): "
                    + DesktopFormat.money(row.currency(), row.paidInMonth()));
        mText.getStyleClass().add("cell-primary");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label yText =
            new Label(
                "Year ("
                    + ym.getYear()
                    + "): "
                    + DesktopFormat.money(row.currency(), row.paidInYear()));
        yText.getStyleClass().add("cell-secondary");
        HBox line = new HBox(14, curBadge, mText, spacer, yText);
        line.setAlignment(Pos.CENTER_LEFT);
        line.getStyleClass().add("breakdown-row");
        ledgerRows.getChildren().add(line);
      }
      content
          .getChildren()
          .add(
              DesktopComponents.panel(
                  "Actual payments by currency",
                  "Currencies remain strictly separate with no exchange-rate conversion",
                  null,
                  ledgerRows));
    }

    // 6-month trend chart
    if (dashboard != null) {
      content
          .getChildren()
          .add(
              DesktopComponents.panel(
                  "Actual payment trend · " + currency,
                  "Recorded payments over the last 6 months",
                  DesktopComponents.tag("Last 6 months"),
                  DesktopComponents.trendChart(dashboard.trends(), currency)));

      // Category breakdown
      List<CategoryTotal> breakdown =
          dashboard.categories().stream().filter(c -> currency.equals(c.currency())).toList();
      Node breakdownBody;
      if (breakdown.isEmpty()) {
        breakdownBody =
            DesktopComponents.emptyState(
                DesktopIcons.WALLET,
                "Add expenses to see a category breakdown",
                "Active recurring bills and subscriptions will be grouped by category here.",
                null,
                null);
      } else {
        VBox rows = new VBox(0);
        BigDecimal divisor =
            currProjection.monthly().compareTo(BigDecimal.ZERO) > 0
                ? currProjection.monthly()
                : BigDecimal.ONE;
        for (CategoryTotal c : breakdown) {
          String hex =
              categories.stream()
                  .filter(k -> k.name().equals(c.category()))
                  .map(CategoryView::color)
                  .findFirst()
                  .orElse("#6056dc");
          String safeHex = DesktopFormat.safeHexColor(hex, "#6056dc");
          HBox chip = DesktopComponents.categoryChip(c.category(), safeHex);
          chip.setPrefWidth(160);

          double pct =
              Math.min(
                  1.0,
                  Math.max(
                      0.04,
                      c.monthly().divide(divisor, 4, RoundingMode.HALF_UP).doubleValue()));
          StackPane track = new StackPane();
          track.getStyleClass().add("progress-track");
          track.setAlignment(Pos.CENTER_LEFT);
          HBox.setHgrow(track, Priority.ALWAYS);
          Region fill = new Region();
          fill.getStyleClass().add("progress-fill");
          fill.setStyle("-fx-background-color: " + safeHex + ";");
          fill.maxWidthProperty().bind(track.widthProperty().multiply(pct));
          fill.prefWidthProperty().bind(track.widthProperty().multiply(pct));
          track.getChildren().add(fill);

          Label amt = new Label(DesktopFormat.money(c.currency(), c.monthly()));
          amt.getStyleClass().add("cell-primary");
          amt.setPrefWidth(120);
          amt.setAlignment(Pos.CENTER_RIGHT);

          HBox row = new HBox(18, chip, track, amt);
          row.setAlignment(Pos.CENTER_LEFT);
          row.getStyleClass().add("breakdown-row");
          rows.getChildren().add(row);
        }
        breakdownBody = rows;
      }
      content
          .getChildren()
          .add(
              DesktopComponents.panel(
                  "Monthly commitments by category",
                  "Normalized monthly estimate by category",
                  DesktopComponents.tag(currency),
                  breakdownBody));
    }

    // Custom CSV date-range export panel
    Label fromLbl = new Label("From:");
    fromLbl.getStyleClass().add("field-label");
    Label toLbl = new Label("To:");
    toLbl.getStyleClass().add("field-label");
    FlowPane exportBar =
        new FlowPane(
            10,
            10,
            fromLbl,
            fromDate,
            toLbl,
            toDate,
            DesktopComponents.primaryButton(
                "Export CSV",
                DesktopIcons.DOWNLOAD,
                () -> onExportCsv.accept(fromDate.getValue(), toDate.getValue())));
    exportBar.setAlignment(Pos.CENTER_LEFT);
    exportBar.getStyleClass().add("filter-bar");

    content
        .getChildren()
        .add(
            DesktopComponents.panel(
                "Export custom payment ledger as CSV",
                "Export recorded payment history for any custom date range",
                DesktopComponents.tag("CSV"),
                exportBar));
  }
}
