package com.brennensubs.billvault.desktop;

import com.brennensubs.billvault.dto.Api.CategoryTotal;
import com.brennensubs.billvault.dto.Api.CategoryView;
import com.brennensubs.billvault.dto.Api.Dashboard;
import com.brennensubs.billvault.dto.Api.ExpenseView;
import com.brennensubs.billvault.dto.Api.MoneyTotal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Dashboard overview workspace displaying KPI cards, upcoming expenses, category mix, and trends. */
final class OverviewWorkspace {
  private final ScrollPane scrollPane;
  private final VBox content = new VBox(20);
  private final ComboBox<String> currencySelector = new ComboBox<>();
  private final Consumer<String> onCurrencyChange;
  private final Consumer<String> onNavigate;
  private final Consumer<ExpenseView> onEdit;
  private final Consumer<ExpenseView> onPay;
  private final Runnable onAdd;
  private boolean updatingCurrencySelector;

  OverviewWorkspace(
      Consumer<String> onCurrencyChange,
      Consumer<String> onNavigate,
      Consumer<ExpenseView> onEdit,
      Consumer<ExpenseView> onPay,
      Runnable onAdd) {
    this.onCurrencyChange = onCurrencyChange;
    this.onNavigate = onNavigate;
    this.onEdit = onEdit;
    this.onPay = onPay;
    this.onAdd = onAdd;

    content.getStyleClass().add("workspace-body");
    content.setMinWidth(0);

    currencySelector.getStyleClass().add("compact-combo");
    currencySelector.setAccessibleText("Dashboard currency");
    currencySelector.setOnAction(
        e -> {
          if (!updatingCurrencySelector && currencySelector.getValue() != null) {
            this.onCurrencyChange.accept(currencySelector.getValue());
          }
        });

    scrollPane = new ScrollPane(content);
    scrollPane.setFitToWidth(true);
    scrollPane.setMinSize(0, 0);
    scrollPane.getStyleClass().add("workspace-scroll");
  }

  Node node() {
    return scrollPane;
  }

  void update(Dashboard dashboard, List<CategoryView> categories, String activeCurrency) {
    content.getChildren().clear();
    if (dashboard == null) {
      return;
    }

    Set<String> currencies = new LinkedHashSet<>();
    if (activeCurrency != null && !activeCurrency.isBlank()) {
      currencies.add(activeCurrency);
    }
    dashboard.totals().forEach(t -> currencies.add(t.currency()));
    if (currencies.isEmpty()) {
      currencies.add("USD");
    }
    String currency =
        activeCurrency != null && currencies.contains(activeCurrency)
            ? activeCurrency
            : currencies.iterator().next();

    updatingCurrencySelector = true;
    currencySelector.setItems(FXCollections.observableArrayList(currencies));
    currencySelector.setValue(currency);
    updatingCurrencySelector = false;

    MoneyTotal total =
        dashboard.totals().stream()
            .filter(t -> currency.equals(t.currency()))
            .findFirst()
            .orElse(new MoneyTotal(currency, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

    // Top overview caption row
    Region liveDot = new Region();
    liveDot.getStyleClass().add("live-dot");
    Label overviewLabel = new Label("YOUR OVERVIEW");
    overviewLabel.getStyleClass().add("section-eyebrow");
    HBox leftCaption = new HBox(7, liveDot, overviewLabel);
    leftCaption.setAlignment(Pos.CENTER_LEFT);

    Region captionSpacer = new Region();
    HBox.setHgrow(captionSpacer, Priority.ALWAYS);
    Label currLabel = new Label("Currency:");
    currLabel.getStyleClass().add("muted-text");
    HBox captionRow = new HBox(10, leftCaption, captionSpacer, currLabel, currencySelector);
    captionRow.setAlignment(Pos.CENTER_LEFT);
    content.getChildren().add(captionRow);

    // 4 KPI Stat Cards in a 4-column responsive GridPane
    GridPane statGrid = new GridPane();
    statGrid.setHgap(14);
    statGrid.setVgap(14);
    for (int i = 0; i < 4; i++) {
      ColumnConstraints cc = new ColumnConstraints();
      cc.setPercentWidth(25);
      cc.setHgrow(Priority.ALWAYS);
      statGrid.getColumnConstraints().add(cc);
    }
    statGrid.add(
        DesktopComponents.statCard(
            "Monthly commitment",
            DesktopFormat.money(currency, total.monthly()),
            "Normalized estimate",
            DesktopIcons.WALLET,
            true),
        0,
        0);
    statGrid.add(
        DesktopComponents.statCard(
            "Annual projection",
            DesktopFormat.money(currency, total.annual()),
            "Based on active expenses",
            DesktopIcons.REPORTS,
            false),
        1,
        0);
    statGrid.add(
        DesktopComponents.statCard(
            "Active expenses",
            String.format("%02d", dashboard.activeCount()),
            dashboard.upcoming7().size() + " due in the next 7 days",
            DesktopIcons.SUBSCRIPTIONS,
            false),
        2,
        0);
    statGrid.add(
        DesktopComponents.statCard(
            "Paid this month",
            DesktopFormat.money(currency, total.paidThisMonth()),
            "Actual recorded payments",
            DesktopIcons.ARROW_DOWN_LEFT,
            false),
        3,
        0);
    content.getChildren().add(statGrid);

    // Overdue banner if any overdue expenses exist
    if (!dashboard.overdue().isEmpty()) {
      int count = dashboard.overdue().size();
      Label alertTitle =
          new Label(
              count + (count == 1 ? " expense needs" : " expenses need") + " your attention");
      alertTitle.getStyleClass().add("overdue-banner-title");
      Label alertSub =
          new Label("Review overdue payments to keep your local vault up to date.");
      alertSub.getStyleClass().add("overdue-banner-subtitle");
      VBox alertText = new VBox(2, alertTitle, alertSub);
      HBox.setHgrow(alertText, Priority.ALWAYS);

      Button reviewBtn =
          DesktopComponents.subtleButton(
              "Review upcoming",
              DesktopIcons.ARROW_UP_RIGHT,
              () -> onNavigate.accept("Upcoming"));
      HBox banner =
          new HBox(
              12,
              DesktopIcons.icon(DesktopIcons.CALENDAR, 20),
              alertText,
              reviewBtn);
      banner.setAlignment(Pos.CENTER_LEFT);
      banner.getStyleClass().add("overdue-banner");
      content.getChildren().add(banner);
    }

    // Main two-column row: Coming up next + Where it goes
    GridPane middleGrid = new GridPane();
    middleGrid.setHgap(18);
    middleGrid.setVgap(18);
    ColumnConstraints leftCol = new ColumnConstraints();
    leftCol.setPercentWidth(62);
    leftCol.setHgrow(Priority.ALWAYS);
    ColumnConstraints rightCol = new ColumnConstraints();
    rightCol.setPercentWidth(38);
    rightCol.setHgrow(Priority.ALWAYS);
    middleGrid.getColumnConstraints().addAll(leftCol, rightCol);

    Button viewCalendarBtn =
        DesktopComponents.subtleButton(
            "View calendar", DesktopIcons.ARROW_UP_RIGHT, () -> onNavigate.accept("Upcoming"));
    Node upcomingBody;
    if (dashboard.upcoming30().isEmpty()) {
      upcomingBody =
          DesktopComponents.emptyState(
              DesktopIcons.CALENDAR,
              "Nothing due in the next 30 days",
              "Add a recurring subscription or household bill to see upcoming due dates here.",
              "Add expense",
              onAdd);
    } else {
      TableView<ExpenseView> upcomingTable = new TableView<>();
      DesktopComponents.configureExpenseTable(
          upcomingTable, true, onEdit, onPay, null, null, onAdd);
      upcomingTable
          .getItems()
          .setAll(dashboard.upcoming30().stream().limit(6).toList());
      upcomingTable.setPrefHeight(270);
      upcomingTable.setMinHeight(180);
      upcomingBody = upcomingTable;
    }
    VBox upcomingPanel =
        DesktopComponents.panel(
            "Coming up next (" + dashboard.upcoming30().size() + ")",
            "Next payments due within 30 days",
            viewCalendarBtn,
            upcomingBody);
    middleGrid.add(upcomingPanel, 0, 0);

    List<CategoryTotal> breakdown =
        dashboard.categories().stream().filter(c -> currency.equals(c.currency())).toList();
    Node breakdownBody = buildCategoryMix(breakdown, categories, total.monthly(), currency);
    VBox spendingPanel =
        DesktopComponents.panel(
            "Where it goes",
            "Estimated monthly cost · " + currency,
            DesktopComponents.tag(currency),
            breakdownBody);
    middleGrid.add(spendingPanel, 1, 0);
    content.getChildren().add(middleGrid);

    // Lower two-column row: Payment activity trend + Insight card
    GridPane lowerGrid = new GridPane();
    lowerGrid.setHgap(18);
    lowerGrid.setVgap(18);
    lowerGrid.getColumnConstraints().addAll(leftCol, rightCol);

    VBox trendPanel =
        DesktopComponents.panel(
            "Payment activity",
            "What you have actually paid over time (" + currency + ")",
            DesktopComponents.tag("Last 6 months"),
            DesktopComponents.trendChart(dashboard.trends(), currency));
    lowerGrid.add(trendPanel, 0, 0);

    VBox insightCard = buildInsightCard();
    lowerGrid.add(insightCard, 1, 0);
    content.getChildren().add(lowerGrid);
  }

  private Node buildCategoryMix(
      List<CategoryTotal> breakdown,
      List<CategoryView> categories,
      BigDecimal monthlyTotal,
      String currency) {
    if (breakdown.isEmpty()) {
      return DesktopComponents.emptyState(
          DesktopIcons.REPORTS,
          "See your spending mix",
          "Categorize your recurring bills and subscriptions to compare monthly commitments.",
          null,
          null);
    }
    VBox box = new VBox(14);
    box.setPadding(new Insets(16, 20, 20, 20));

    // Summary header pill inside the category card
    Label totalCaption = new Label("MONTHLY TOTAL");
    totalCaption.getStyleClass().add("section-eyebrow");
    Label totalValue = new Label(DesktopFormat.money(currency, monthlyTotal));
    totalValue.getStyleClass().add("mix-total-value");
    VBox summaryBox = new VBox(3, totalCaption, totalValue);
    summaryBox.setAlignment(Pos.CENTER);
    summaryBox.getStyleClass().add("mix-summary-box");
    box.getChildren().add(summaryBox);

    BigDecimal divisor =
        monthlyTotal != null && monthlyTotal.compareTo(BigDecimal.ZERO) > 0
            ? monthlyTotal
            : BigDecimal.ONE;

    for (CategoryTotal item : breakdown) {
      String hex =
          categories.stream()
              .filter(c -> c.name().equals(item.category()))
              .map(CategoryView::color)
              .findFirst()
              .orElse("#6056dc");
      String safeHex = DesktopFormat.safeHexColor(hex, "#6056dc");

      HBox topRow = new HBox(8);
      topRow.setAlignment(Pos.CENTER_LEFT);
      HBox chip = DesktopComponents.categoryChip(item.category(), safeHex);
      Region spacer = new Region();
      HBox.setHgrow(spacer, Priority.ALWAYS);
      Label amt = new Label(DesktopFormat.money(item.currency(), item.monthly()));
      amt.getStyleClass().add("cell-primary");
      topRow.getChildren().addAll(chip, spacer, amt);

      double pct =
          Math.min(
              1.0,
              Math.max(
                  0.04,
                  item.monthly().divide(divisor, 4, RoundingMode.HALF_UP).doubleValue()));
      StackPane track = new StackPane();
      track.getStyleClass().add("progress-track");
      track.setAlignment(Pos.CENTER_LEFT);
      Region fill = new Region();
      fill.getStyleClass().add("progress-fill");
      fill.setStyle("-fx-background-color: " + safeHex + ";");
      fill.maxWidthProperty().bind(track.widthProperty().multiply(pct));
      fill.prefWidthProperty().bind(track.widthProperty().multiply(pct));
      track.getChildren().add(fill);

      VBox catRow = new VBox(6, topRow, track);
      box.getChildren().add(catRow);
    }
    return box;
  }

  private VBox buildInsightCard() {
    StackPane shield = new StackPane(DesktopIcons.icon(DesktopIcons.SHIELD, 22));
    shield.getStyleClass().add("insight-icon-badge");

    Label eyebrow = new Label("A CLEARER PICTURE");
    eyebrow.getStyleClass().add("section-eyebrow");

    Label title = new Label("Small expenses.\nReal perspective.");
    title.getStyleClass().add("insight-title");
    title.setWrapText(true);

    Label body =
        new Label(
            "Your desktop vault separates normalized recurring commitments from the actual"
                + " payments you record each month—stored 100% locally on your machine.");
    body.getStyleClass().add("insight-body");
    body.setWrapText(true);

    Button exploreBtn =
        DesktopComponents.subtleButton(
            "Explore your reports",
            DesktopIcons.ARROW_UP_RIGHT,
            () -> onNavigate.accept("Reports"));

    Region spacer = new Region();
    VBox.setVgrow(spacer, Priority.ALWAYS);

    VBox card = new VBox(10, shield, eyebrow, title, body, spacer, exploreBtn);
    card.getStyleClass().add("insight-card");
    return card;
  }
}
