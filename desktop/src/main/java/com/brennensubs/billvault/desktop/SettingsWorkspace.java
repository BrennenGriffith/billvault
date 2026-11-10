package com.brennensubs.billvault.desktop;

import com.brennensubs.billvault.dto.Api.CategoryRequest;
import com.brennensubs.billvault.dto.Api.CategoryView;
import com.brennensubs.billvault.dto.Api.SettingsRequest;
import com.brennensubs.billvault.dto.Api.SettingsView;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;

/** Workspace preferences, category management, and local vault storage information. */
final class SettingsWorkspace {
  private final ScrollPane scrollPane;
  private final VBox content = new VBox(18);
  private final Path directory;
  private final Consumer<SettingsRequest> onSaveSettings;
  private final Consumer<CategoryRequest> onAddCategory;
  private final Consumer<String> onStatusUpdate;

  SettingsWorkspace(
      Path directory,
      Consumer<SettingsRequest> onSaveSettings,
      Consumer<CategoryRequest> onAddCategory,
      Consumer<String> onStatusUpdate) {
    this.directory = directory;
    this.onSaveSettings = onSaveSettings;
    this.onAddCategory = onAddCategory;
    this.onStatusUpdate = onStatusUpdate;

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

  void update(SettingsView settings, List<CategoryView> categories) {
    content.getChildren().clear();
    if (settings == null) {
      return;
    }

    GridPane grid = new GridPane();
    grid.setHgap(18);
    grid.setVgap(18);
    ColumnConstraints leftCol = new ColumnConstraints();
    leftCol.setPercentWidth(54);
    leftCol.setHgrow(Priority.ALWAYS);
    ColumnConstraints rightCol = new ColumnConstraints();
    rightCol.setPercentWidth(46);
    rightCol.setHgrow(Priority.ALWAYS);
    grid.getColumnConstraints().addAll(leftCol, rightCol);

    // Preferences Panel
    Set<String> currencies =
        new LinkedHashSet<>(List.of("USD", "EUR", "GBP", "CAD", "AUD", "INR", "JPY", "CHF"));
    if (settings.currency() != null && !settings.currency().isBlank()) {
      currencies.add(settings.currency());
    }
    ComboBox<String> currencyBox =
        new ComboBox<>(FXCollections.observableArrayList(currencies));
    currencyBox.setEditable(true);
    currencyBox.setValue(settings.currency());
    currencyBox.setMaxWidth(Double.MAX_VALUE);

    ComboBox<String> themeBox =
        new ComboBox<>(FXCollections.observableArrayList("light", "dark"));
    themeBox.setValue("dark".equalsIgnoreCase(settings.theme()) ? "dark" : "light");
    themeBox.setMaxWidth(Double.MAX_VALUE);
    themeBox.setConverter(
        new StringConverter<>() {
          @Override
          public String toString(String s) {
            return "dark".equalsIgnoreCase(s) ? "Dark" : "Light";
          }

          @Override
          public String fromString(String s) {
            return "Dark".equalsIgnoreCase(s) ? "dark" : "light";
          }
        });

    Spinner<Integer> reminderSpinner = new Spinner<>();
    reminderSpinner.setValueFactory(
        new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 90, settings.reminderDays()));
    reminderSpinner.setEditable(true);
    reminderSpinner.setMaxWidth(Double.MAX_VALUE);

    Label curLbl = new Label("Default currency (ISO 3-letter)");
    curLbl.getStyleClass().add("field-label");
    curLbl.setLabelFor(currencyBox);

    Label themeLbl = new Label("Appearance");
    themeLbl.getStyleClass().add("field-label");
    themeLbl.setLabelFor(themeBox);

    Label remLbl = new Label("Default reminder lead time (days)");
    remLbl.getStyleClass().add("field-label");
    remLbl.setLabelFor(reminderSpinner);

    Label hint =
        new Label(
            "Defaults apply to newly created expenses. Edit an individual expense to customize"
                + " its reminder.");
    hint.getStyleClass().add("muted-text");
    hint.setWrapText(true);

    Button savePrefsBtn =
        DesktopComponents.primaryButton(
            "Save preferences",
            DesktopIcons.CHECK,
            () -> {
              String rawCurrency =
                  currencyBox.getEditor().getText() != null
                          && !currencyBox.getEditor().getText().isBlank()
                      ? currencyBox.getEditor().getText()
                      : currencyBox.getValue();
              onSaveSettings.accept(
                  new SettingsRequest(
                      rawCurrency == null
                          ? "USD"
                          : rawCurrency.trim().toUpperCase(Locale.ROOT),
                      themeBox.getValue(),
                      reminderSpinner.getValue()));
            });

    VBox prefsBody =
        new VBox(
            12,
            new VBox(5, curLbl, currencyBox),
            new VBox(5, themeLbl, themeBox),
            new VBox(5, remLbl, reminderSpinner),
            hint,
            savePrefsBtn);
    prefsBody.setPadding(new Insets(18, 20, 20, 20));

    VBox prefsPanel =
        DesktopComponents.panel(
            "Workspace preferences",
            "Defaults and visual appearance for your personal desktop vault",
            null,
            prefsBody);

    // Categories Panel
    VBox catBody = new VBox(10);
    catBody.setPadding(new Insets(14, 20, 20, 20));
    if (categories != null) {
      for (CategoryView c : categories) {
        HBox chip = DesktopComponents.categoryChip(c.name(), c.color());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label colorHex = new Label(c.color());
        colorHex.getStyleClass().add("cell-muted");
        HBox row = new HBox(10, chip, spacer, colorHex);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("category-list-row");
        catBody.getChildren().add(row);
      }
    }

    TextField newCatName = new TextField();
    newCatName.setPromptText("New category name…");
    HBox.setHgrow(newCatName, Priority.ALWAYS);

    ColorPicker newCatColor = new ColorPicker(Color.web("#6056dc"));
    newCatColor.setPrefWidth(110);

    Button addCatBtn =
        DesktopComponents.secondaryButton(
            "Add",
            DesktopIcons.PLUS,
            () -> {
              Color c = newCatColor.getValue();
              String hex =
                  String.format(
                      "#%02x%02x%02x",
                      Math.round(c.getRed() * 255),
                      Math.round(c.getGreen() * 255),
                      Math.round(c.getBlue() * 255));
              onAddCategory.accept(new CategoryRequest(newCatName.getText(), hex));
            });

    HBox addCatForm = new HBox(8, newCatName, newCatColor, addCatBtn);
    addCatForm.setAlignment(Pos.CENTER_LEFT);
    VBox.setMargin(addCatForm, new Insets(8, 0, 0, 0));
    catBody.getChildren().add(addCatForm);

    VBox categoriesPanel =
        DesktopComponents.panel(
            "Expense categories",
            "Color-coded categories for grouping subscriptions and household bills",
            DesktopComponents.tag(
                (categories == null ? 0 : categories.size()) + " categories"),
            catBody);

    grid.add(prefsPanel, 0, 0);
    grid.add(categoriesPanel, 1, 0);
    content.getChildren().add(grid);

    // Local storage & backup panel
    String normalizedPath = directory.toAbsolutePath().normalize().toString();
    Label pathLabel = new Label(normalizedPath);
    pathLabel.getStyleClass().add("storage-path-box");
    pathLabel.setWrapText(true);
    pathLabel.setMaxWidth(Double.MAX_VALUE);
    HBox.setHgrow(pathLabel, Priority.ALWAYS);

    Button copyPathBtn =
        DesktopComponents.secondaryButton(
            "Copy folder path",
            DesktopIcons.FOLDER,
            () -> {
              ClipboardContent clip = new ClipboardContent();
              clip.putString(normalizedPath);
              Clipboard.getSystemClipboard().setContent(clip);
              onStatusUpdate.accept("Copied database folder path to clipboard.");
            });

    HBox pathRow = new HBox(10, pathLabel, copyPathBtn);
    pathRow.setAlignment(Pos.CENTER_LEFT);

    Label backupGuide =
        new Label(
            "• To back up: close BillVault Desktop completely, then copy billvault.mv.db from the"
                + " folder above to another drive or device.\n"
                + "• To restore: close BillVault Desktop, preserve a copy of your current file,"
                + " then copy your verified backup into this folder.\n"
                + "• Security: the local H2 database is not encrypted; your operating-system"
                + " user account protects access.\n"
                + "• Edition isolation: this standalone desktop vault is separate from the"
                + " self-hosted web edition; there is no automatic synchronization.");
    backupGuide.getStyleClass().add("muted-text");
    backupGuide.setWrapText(true);

    Label licenseFooter =
        new Label(
            "BillVault Desktop · MIT License · Native JavaFX + embedded H2 · No account, cloud"
                + " service, bank connection, or saved administrator credentials.");
    licenseFooter.getStyleClass().add("cell-muted");
    licenseFooter.setWrapText(true);

    VBox storageBody =
        new VBox(12, pathRow, new Separator(), backupGuide, new Separator(), licenseFooter);
    storageBody.setPadding(new Insets(18, 20, 20, 20));

    VBox storagePanel =
        DesktopComponents.panel(
            "Your local database folder & backup guide",
            "All records live directly on your computer outside the application installation"
                + " directory",
            DesktopComponents.tag("Local H2"),
            storageBody);
    content.getChildren().add(storagePanel);
  }
}
