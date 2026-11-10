package com.brennensubs.billvault.desktop;

import com.brennensubs.billvault.dto.Api.CategoryView;
import com.brennensubs.billvault.dto.Api.Dashboard;
import com.brennensubs.billvault.dto.Api.ExpenseView;
import com.brennensubs.billvault.dto.Api.NotificationView;
import com.brennensubs.billvault.dto.Api.Page;
import com.brennensubs.billvault.dto.Api.PaymentView;
import com.brennensubs.billvault.dto.Api.ReportTotal;
import com.brennensubs.billvault.dto.Api.SettingsRequest;
import com.brennensubs.billvault.dto.Api.SettingsView;
import com.brennensubs.billvault.model.ExpenseKind;
import com.brennensubs.billvault.model.ExpenseStatus;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/** Native JavaFX application shell coordinating sidebar navigation, workspaces, and storage tasks. */
public final class DesktopApplication extends Application {
  private static final List<NavSpec> NAVIGATION =
      List.of(
          new NavSpec(
              "Overview",
              DesktopIcons.DASHBOARD,
              "Your money, in view.",
              "A little clarity for everything that comes around."),
          new NavSpec(
              "Expenses",
              DesktopIcons.SUBSCRIPTIONS,
              "Recurring expenses.",
              "Manage all your subscriptions and household bills in one place."),
          new NavSpec(
              "Upcoming",
              DesktopIcons.CALENDAR,
              "Stay one step ahead.",
              "Projected due dates and overdue alerts for your active recurring expenses."),
          new NavSpec(
              "Payments",
              DesktopIcons.HISTORY,
              "Recorded payments.",
              "A clear ledger of what you have actually paid."),
          new NavSpec(
              "Reports & export",
              DesktopIcons.REPORTS,
              "The bigger picture.",
              "Projected commitments and recorded payments, clearly separated."),
          new NavSpec(
              "Reminders",
              DesktopIcons.BELL,
              "In-app reminders.",
              "Upcoming due-date notifications generated locally while using your vault."),
          new NavSpec(
              "Storage & preferences",
              DesktopIcons.SETTINGS,
              "Make it yours.",
              "Workspace defaults, categories, appearance, and local database backup guide."));

  private record NavSpec(String id, String icon, String heading, String subtitle) {}

  private record Snapshot(
      Page<ExpenseView> expenses,
      List<PaymentView> payments,
      Dashboard dashboard,
      List<CategoryView> categories,
      List<NotificationView> notices,
      SettingsView settings,
      List<Map<String, Object>> calendar,
      List<ReportTotal> reportTotals) {}

  private final ExecutorService worker =
      Executors.newSingleThreadExecutor(r -> new Thread(r, "billvault-storage"));
  private volatile DesktopStore store;
  private Stage stage;
  private final BorderPane root = new BorderPane();
  private final Map<String, Button> navButtons = new LinkedHashMap<>();
  private final Map<String, Label> navBadges = new LinkedHashMap<>();
  private final Label breadcrumbCurrent = new Label("Overview");
  private final Label pageEyebrow = new Label();
  private final Label pageHeading = new Label();
  private final Label pageSubtitle = new Label();
  private final StackPane workspaceHolder = new StackPane();
  private final Label statusLabel = new Label("Opening your local vault…");
  private final Label statusCurrencyTag = DesktopComponents.tag("USD");
  private Button themeToggleButton;
  private Region bellUnreadDot;

  private OverviewWorkspace overviewWorkspace;
  private ExpensesWorkspace expensesWorkspace;
  private UpcomingWorkspace upcomingWorkspace;
  private PaymentsWorkspace paymentsWorkspace;
  private ReportsWorkspace reportsWorkspace;
  private RemindersWorkspace remindersWorkspace;
  private SettingsWorkspace settingsWorkspace;

  private Path directory;
  private String activeTab = "Overview";
  private List<CategoryView> categories = List.of();
  private SettingsView currentSettings = new SettingsView("USD", "light", 3);
  private boolean darkTheme;

  @Override
  public void start(Stage primary) {
    stage = primary;
    directory =
        getParameters().getNamed().containsKey("data-dir")
            ? Path.of(getParameters().getNamed().get("data-dir"))
            : DesktopStore.defaultDirectory();

    root.getStyleClass().add("app-shell");
    root.setMinSize(0, 0);
    root.setCenter(buildBootView());
    root.setBottom(buildStatusBar());

    Scene scene = new Scene(root);
    scene
        .getStylesheets()
        .add(Objects.requireNonNull(getClass().getResource("desktop.css")).toExternalForm());

    stage.setTitle("BillVault Desktop");
    stage.initStyle(StageStyle.DECORATED);
    stage.setResizable(true);
    stage
        .getIcons()
        .add(
            new Image(
                Objects.requireNonNull(getClass().getResourceAsStream("billvault.png"))));
    stage.setScene(scene);

    Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
    DesktopWindowSizing.fit(stage, visualBounds);
    // Reapply after the native window exists so decorations cannot push it off-screen.
    stage.setOnShown(e -> Platform.runLater(() -> DesktopWindowSizing.fit(stage, visualBounds)));
    stage.show();

    work(
        () -> {
          store = new DesktopStore(directory);
          return store;
        },
        opened -> {
          buildShell();
          refresh();
        });
  }

  private Node buildBootView() {
    StackPane logoMark = new StackPane(new Label("B"));
    logoMark.getStyleClass().add("logo-mark-large");
    Label title = new Label("Opening your local vault…");
    title.getStyleClass().add("boot-title");
    Label sub = new Label("Starting embedded H2 storage · No cloud or server required");
    sub.getStyleClass().add("muted-text");
    VBox box = new VBox(10, logoMark, title, sub);
    box.setAlignment(Pos.CENTER);
    box.getStyleClass().add("boot-pane");
    return box;
  }

  private Node buildStatusBar() {
    Region dot = new Region();
    dot.getStyleClass().add("live-dot");
    statusLabel.getStyleClass().add("status-text");
    statusLabel.setWrapText(false);
    statusLabel.setMinWidth(0);

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    Label privacyNote = new Label("LOCAL STORAGE · NO HOSTING REQUIRED");
    privacyNote.getStyleClass().add("status-privacy");

    HBox bar = new HBox(10, dot, statusLabel, spacer, privacyNote, statusCurrencyTag);
    bar.setAlignment(Pos.CENTER_LEFT);
    bar.getStyleClass().add("status-bar");
    bar.setMinWidth(0);
    return bar;
  }

  private void buildShell() {
    overviewWorkspace =
        new OverviewWorkspace(
            this::updateDefaultCurrency,
            this::navigateTo,
            this::openEditExpense,
            this::openRecordPayment,
            () -> openEditExpense(null));

    expensesWorkspace =
        new ExpensesWorkspace(
            this::refresh,
            () -> openEditExpense(null),
            this::openAddCategory,
            this::openEditExpense,
            this::openRecordPayment,
            this::openReminderSettings,
            this::confirmAndDeleteExpense,
            this::showError);

    upcomingWorkspace =
        new UpcomingWorkspace(
            ym -> refresh(),
            this::openEditExpense,
            this::openRecordPayment,
            () -> openEditExpense(null));

    paymentsWorkspace = new PaymentsWorkspace();

    reportsWorkspace =
        new ReportsWorkspace(
            date -> {
              if (date == null) {
                showError("Choose a valid report date.");
                return;
              }
              refresh();
            },
            this::exportCsv);

    remindersWorkspace =
        new RemindersWorkspace(
            this::refresh,
            notice ->
                work(
                    () -> {
                      store.service.readNotification(notice.id());
                      return true;
                    },
                    ok -> refresh()),
            this::showError);

    settingsWorkspace =
        new SettingsWorkspace(
            directory,
            req ->
                work(
                    () -> store.service.settings(store.validated(req)),
                    saved -> {
                      applyTheme("dark".equalsIgnoreCase(saved.theme()));
                      refresh();
                      statusLabel.setText("Preferences saved · " + directory.toAbsolutePath().normalize());
                    }),
            catReq ->
                work(
                    () -> store.service.category(store.validated(catReq)),
                    saved -> refresh()),
            msg -> statusLabel.setText(msg));

    Node sidebar = buildSidebar();
    Node mainArea = buildMainArea();

    root.setTop(null);
    root.setLeft(sidebar);
    root.setCenter(mainArea);
    navigateTo("Overview");
  }

  private Node buildSidebar() {
    Label bMark = new Label("B");
    bMark.getStyleClass().add("logo-badge-text");
    StackPane logoBadge = new StackPane(bMark);
    logoBadge.getStyleClass().add("logo-badge");

    Label brandText = new Label("BillVault");
    brandText.getStyleClass().add("sidebar-brand-text");
    Label brandDot = new Label(".");
    brandDot.getStyleClass().add("sidebar-brand-dot");
    HBox brandBox = new HBox(0, brandText, brandDot);
    brandBox.setAlignment(Pos.BASELINE_LEFT);

    HBox logoRow = new HBox(10, logoBadge, brandBox);
    logoRow.setAlignment(Pos.CENTER_LEFT);
    logoRow.getStyleClass().add("sidebar-logo-row");

    // Workspace badge card
    StackPane wsIcon = new StackPane(DesktopIcons.icon(DesktopIcons.WALLET, 16));
    wsIcon.getStyleClass().add("workspace-card-icon");
    Label wsTitle = new Label("Personal vault");
    wsTitle.getStyleClass().add("workspace-card-title");
    Label wsSub = new Label("Local desktop edition");
    wsSub.getStyleClass().add("workspace-card-subtitle");
    VBox wsText = new VBox(1, wsTitle, wsSub);
    HBox.setHgrow(wsText, Priority.ALWAYS);
    HBox wsCard =
        new HBox(10, wsIcon, wsText, DesktopIcons.icon(DesktopIcons.SHIELD, 15));
    wsCard.setAlignment(Pos.CENTER_LEFT);
    wsCard.getStyleClass().add("workspace-card");

    Label navHeader = new Label("WORKSPACE");
    navHeader.getStyleClass().add("sidebar-section-label");

    VBox navList = new VBox(4);
    navButtons.clear();
    navBadges.clear();
    for (NavSpec spec : NAVIGATION) {
      Node icon = DesktopIcons.icon(spec.icon(), 17);
      Label name = new Label(spec.id());
      name.getStyleClass().add("nav-item-label");
      Region spacer = new Region();
      HBox.setHgrow(spacer, Priority.ALWAYS);

      Label badge = new Label("0");
      badge.getStyleClass().add("nav-badge");
      badge.setVisible(false);
      badge.setManaged(false);
      navBadges.put(spec.id(), badge);

      HBox row = new HBox(11, icon, name, spacer, badge);
      row.setAlignment(Pos.CENTER_LEFT);

      Button navBtn = new Button();
      navBtn.setGraphic(row);
      navBtn.setMaxWidth(Double.MAX_VALUE);
      navBtn.getStyleClass().add("nav-item");
      navBtn.setOnAction(e -> navigateTo(spec.id()));
      navButtons.put(spec.id(), navBtn);
      navList.getChildren().add(navBtn);
    }

    Region spacer = new Region();
    VBox.setVgrow(spacer, Priority.ALWAYS);

    // Bottom peace-of-mind card
    Label noteTitle = new Label("A little peace of mind.");
    noteTitle.getStyleClass().add("vault-note-title");
    Label noteBody = new Label("Your recurring expenses, organized privately on your computer.");
    noteBody.getStyleClass().add("vault-note-body");
    noteBody.setWrapText(true);
    VBox vaultNote =
        new VBox(
            6,
            DesktopIcons.icon(DesktopIcons.SHIELD, 18),
            noteTitle,
            noteBody);
    vaultNote.getStyleClass().add("vault-note");

    VBox sidebar = new VBox(14, logoRow, wsCard, navHeader, navList, spacer, vaultNote);
    sidebar.getStyleClass().add("sidebar");
    sidebar.setMinHeight(0);
    return sidebar;
  }

  private Node buildMainArea() {
    // Top bar
    Label crumbRoot = new Label("Personal vault");
    crumbRoot.getStyleClass().add("breadcrumb-muted");
    Label crumbSlash = new Label("/");
    crumbSlash.getStyleClass().add("breadcrumb-muted");
    breadcrumbCurrent.getStyleClass().add("breadcrumb-current");
    HBox breadcrumb = new HBox(8, crumbRoot, crumbSlash, breadcrumbCurrent);
    breadcrumb.setAlignment(Pos.CENTER_LEFT);

    Region topSpacer = new Region();
    HBox.setHgrow(topSpacer, Priority.ALWAYS);

    Region privateDot = new Region();
    privateDot.getStyleClass().add("live-dot");
    Label privateLabel = new Label("Private local workspace");
    privateLabel.getStyleClass().add("private-workspace-label");
    HBox privatePill = new HBox(6, privateDot, privateLabel);
    privatePill.setAlignment(Pos.CENTER_LEFT);

    themeToggleButton =
        DesktopComponents.iconButton(
            "Toggle light/dark appearance", DesktopIcons.MOON, this::toggleTheme);

    Button bellBtn =
        DesktopComponents.iconButton(
            "View reminders", DesktopIcons.BELL, () -> navigateTo("Reminders"));
    bellUnreadDot = new Region();
    bellUnreadDot.getStyleClass().add("bell-unread-dot");
    bellUnreadDot.setVisible(false);
    StackPane bellWrap = new StackPane(bellBtn, bellUnreadDot);
    StackPane.setAlignment(bellUnreadDot, Pos.TOP_RIGHT);
    StackPane.setMargin(bellUnreadDot, new Insets(6, 6, 0, 0));

    Button addExpenseBtn =
        DesktopComponents.primaryButton(
            "Add expense", DesktopIcons.PLUS, () -> openEditExpense(null));

    HBox topActions =
        new HBox(10, privatePill, themeToggleButton, bellWrap, addExpenseBtn);
    topActions.setAlignment(Pos.CENTER_RIGHT);

    HBox topbar = new HBox(16, breadcrumb, topSpacer, topActions);
    topbar.setAlignment(Pos.CENTER_LEFT);
    topbar.getStyleClass().add("topbar");

    // Page heading banner
    pageEyebrow.getStyleClass().add("page-eyebrow");
    pageHeading.getStyleClass().add("page-title");
    pageSubtitle.getStyleClass().add("page-subtitle");
    pageSubtitle.setWrapText(true);
    VBox headingBox = new VBox(4, pageEyebrow, pageHeading, pageSubtitle);
    headingBox.getStyleClass().add("page-heading-box");

    workspaceHolder.setMinSize(0, 0);
    VBox.setVgrow(workspaceHolder, Priority.ALWAYS);

    VBox centerColumn = new VBox(0, headingBox, workspaceHolder);
    centerColumn.getStyleClass().add("main-content-column");
    centerColumn.setMinSize(0, 0);

    BorderPane mainShell = new BorderPane();
    mainShell.getStyleClass().add("main-shell");
    mainShell.setMinSize(0, 0);
    mainShell.setTop(topbar);
    mainShell.setCenter(centerColumn);
    return mainShell;
  }

  private void navigateTo(String tabId) {
    String resolved =
        NAVIGATION.stream().anyMatch(n -> n.id().equals(tabId))
            ? tabId
            : ("Reports".equals(tabId) ? "Reports & export" : "Overview");
    activeTab = resolved;
    breadcrumbCurrent.setText(resolved);

    for (Map.Entry<String, Button> entry : navButtons.entrySet()) {
      if (entry.getKey().equals(resolved)) {
        if (!entry.getValue().getStyleClass().contains("selected")) {
          entry.getValue().getStyleClass().add("selected");
        }
      } else {
        entry.getValue().getStyleClass().remove("selected");
      }
    }

    NavSpec spec =
        NAVIGATION.stream()
            .filter(n -> n.id().equals(resolved))
            .findFirst()
            .orElse(NAVIGATION.getFirst());
    pageEyebrow.setText(
        "Overview".equals(resolved)
            ? DesktopFormat.todayEyebrow(LocalDate.now())
            : "YOUR PERSONAL VAULT");
    pageHeading.setText(spec.heading());
    pageSubtitle.setText(spec.subtitle());

    Node target =
        switch (resolved) {
          case "Expenses" -> expensesWorkspace.node();
          case "Upcoming" -> upcomingWorkspace.node();
          case "Payments" -> paymentsWorkspace.node();
          case "Reports & export" -> reportsWorkspace.node();
          case "Reminders" -> remindersWorkspace.node();
          case "Storage & preferences" -> settingsWorkspace.node();
          default -> overviewWorkspace.node();
        };
    workspaceHolder.getChildren().setAll(target);
  }

  private void applyTheme(boolean dark) {
    darkTheme = dark;
    if (dark) {
      if (!root.getStyleClass().contains("dark")) {
        root.getStyleClass().add("dark");
      }
    } else {
      root.getStyleClass().remove("dark");
    }
    if (themeToggleButton != null) {
      themeToggleButton.setGraphic(
          DesktopIcons.icon(dark ? DesktopIcons.SUN : DesktopIcons.MOON, 17));
    }
  }

  private void toggleTheme() {
    String nextTheme = darkTheme ? "light" : "dark";
    applyTheme(!darkTheme);
    SettingsRequest req =
        new SettingsRequest(
            currentSettings.currency(), nextTheme, currentSettings.reminderDays());
    work(
        () -> store.service.settings(store.validated(req)),
        saved -> {
          currentSettings = saved;
          settingsWorkspace.update(saved, categories);
        });
  }

  private void updateDefaultCurrency(String newCurrency) {
    if (newCurrency == null
        || newCurrency.isBlank()
        || newCurrency.equals(currentSettings.currency())) {
      return;
    }
    SettingsRequest req =
        new SettingsRequest(
            newCurrency, currentSettings.theme(), currentSettings.reminderDays());
    work(
        () -> store.service.settings(store.validated(req)),
        saved -> {
          currentSettings = saved;
          refresh();
        });
  }

  private void refresh() {
    String query = expensesWorkspace.searchQuery();
    ExpenseKind kind = expensesWorkspace.selectedKind();
    ExpenseStatus state = expensesWorkspace.selectedStatus();
    Long categoryId = expensesWorkspace.selectedCategoryId();
    String sort = expensesWorkspace.selectedSort();
    int requestedPage = expensesWorkspace.currentPage();
    YearMonth calMonth = upcomingWorkspace.selectedMonth();
    LocalDate repDate = reportsWorkspace.selectedReportDate();

    work(
        () -> {
          store.service.refreshNotifications();
          return new Snapshot(
              store.service.list(query, kind, state, categoryId, sort, requestedPage, 25),
              store.service.payments(),
              store.service.dashboard(),
              store.service.categories(),
              store.service.notifications(),
              store.service.settings(),
              store.service.calendar(calMonth.atDay(1), calMonth.atEndOfMonth()),
              store.service.report(repDate.getYear(), repDate.getMonthValue()));
        },
        s -> {
          currentSettings = s.settings();
          categories = s.categories();
          applyTheme("dark".equalsIgnoreCase(currentSettings.theme()));
          statusCurrencyTag.setText(currentSettings.currency());

          // Update nav badges
          int upcoming7Count = s.dashboard().upcoming7().size();
          updateNavBadge("Upcoming", upcoming7Count);
          long unreadCount = s.notices().stream().filter(n -> !n.read()).count();
          updateNavBadge("Reminders", (int) unreadCount);
          if (bellUnreadDot != null) {
            bellUnreadDot.setVisible(unreadCount > 0);
          }

          // Update all workspaces
          overviewWorkspace.update(s.dashboard(), s.categories(), currentSettings.currency());
          expensesWorkspace.update(s.expenses(), s.categories());
          upcomingWorkspace.update(s.dashboard().overdue(), s.calendar());
          paymentsWorkspace.update(s.payments());
          reportsWorkspace.update(
              s.dashboard(), s.categories(), s.reportTotals(), currentSettings.currency());
          remindersWorkspace.update(s.notices());
          settingsWorkspace.update(s.settings(), s.categories());

          statusLabel.setText("Saved locally · " + directory.toAbsolutePath().normalize());

          if ("true".equals(getParameters().getNamed().get("smoke-test"))) {
            Rectangle2D frame =
                new Rectangle2D(
                    stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
            if (stage.getStyle() != StageStyle.DECORATED
                || !stage.isResizable()
                || !DesktopWindowSizing.contains(Screen.getPrimary().getVisualBounds(), frame)) {
              throw new IllegalStateException(
                  "Desktop frame extends beyond the usable screen or lacks native window"
                      + " controls.");
            }
            PauseTransition finish = new PauseTransition(Duration.seconds(2));
            finish.setOnFinished(
                event -> {
                  stage.close();
                  Platform.exit();
                });
            finish.play();
          }
        });
  }

  private void updateNavBadge(String navId, int count) {
    Label badge = navBadges.get(navId);
    if (badge != null) {
      badge.setText(String.valueOf(count));
      badge.setVisible(count > 0);
      badge.setManaged(count > 0);
    }
  }

  private void openEditExpense(ExpenseView original) {
    DesktopDialogs.showExpenseDialog(
        stage,
        darkTheme,
        store,
        original,
        expensesWorkspace != null ? expensesWorkspace.selectedKind() : null,
        currentSettings.currency(),
        categories,
        request ->
            work(
                () -> store.service.save(original == null ? null : original.id(), request),
                saved -> refresh()));
  }

  private void openRecordPayment(ExpenseView expense) {
    DesktopDialogs.showPaymentDialog(
        stage,
        darkTheme,
        store,
        expense,
        request -> work(() -> store.service.pay(expense.id(), request), paid -> refresh()));
  }

  private void openAddCategory() {
    DesktopDialogs.showCategoryDialog(
        stage,
        darkTheme,
        store,
        request -> work(() -> store.service.category(request), created -> refresh()));
  }

  private void openReminderSettings(ExpenseView expense) {
    work(
        () -> store.service.reminder(expense.id()),
        current ->
            DesktopDialogs.showReminderDialog(
                stage,
                darkTheme,
                store,
                expense,
                current,
                request ->
                    work(
                        () -> store.service.reminder(expense.id(), request),
                        updated -> refresh())));
  }

  private void confirmAndDeleteExpense(ExpenseView expense) {
    if (DesktopDialogs.confirmDelete(stage, darkTheme, expense)) {
      work(
          () -> {
            store.service.delete(expense.id());
            return true;
          },
          deleted -> refresh());
    }
  }

  private void exportCsv(LocalDate from, LocalDate to) {
    if (from == null || to == null || to.isBefore(from)) {
      showError("Choose a valid date range (the end date must be on or after the start date).");
      return;
    }
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Export payments CSV");
    chooser.setInitialFileName("billvault-payments-" + from + "-to-" + to + ".csv");
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
    var file = chooser.showSaveDialog(stage);
    if (file == null) {
      return;
    }
    work(
        () -> {
          Files.writeString(file.toPath(), store.service.csv(from, to), StandardCharsets.UTF_8);
          return file;
        },
        saved -> statusLabel.setText("Exported CSV: " + saved.getAbsolutePath()));
  }

  private void showError(String message) {
    DesktopDialogs.showError(stage, darkTheme, message);
  }

  @FunctionalInterface
  private interface Job<T> {
    T run() throws Exception;
  }

  private <T> void work(Job<T> job, Consumer<T> success) {
    root.setDisable(true);
    statusLabel.setText("Working…");
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
                    showError(DesktopDialogs.formatErrorMessage(ex));
                  }
                });
          } catch (Exception ex) {
            Platform.runLater(
                () -> {
                  root.setDisable(false);
                  statusLabel.setText("Operation failed; your existing records were preserved.");
                  showError(DesktopDialogs.formatErrorMessage(ex));
                });
          }
        });
  }

  @Override
  public void stop() {
    worker.submit(
        () -> {
          if (store != null) {
            store.close();
          }
        });
    worker.shutdown();
  }
}
