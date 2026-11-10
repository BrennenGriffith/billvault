package com.brennensubs.billvault.desktop;

import com.brennensubs.billvault.dto.Api.CategoryView;
import com.brennensubs.billvault.dto.Api.ExpenseView;
import com.brennensubs.billvault.dto.Api.Page;
import com.brennensubs.billvault.model.ExpenseKind;
import com.brennensubs.billvault.model.ExpenseStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/** Searchable, filterable, paginated expense management workspace. */
final class ExpensesWorkspace {
  private final VBox root = new VBox(14);
  private final TableView<ExpenseView> table = new TableView<>();
  private final TextField searchField = new TextField();
  private final ComboBox<ExpenseKind> kindFilter = new ComboBox<>();
  private final ComboBox<ExpenseStatus> statusFilter = new ComboBox<>();
  private final ComboBox<CategoryView> categoryFilter = new ComboBox<>();
  private final ComboBox<String> sortSelector = new ComboBox<>();
  private final Label pageInfo = new Label("Page 1 of 1 · 0 expenses");
  private final Button prevButton;
  private final Button nextButton;
  private final Runnable onQueryChange;
  private boolean suppressEvents;
  private int page;
  private int totalPages = 1;

  ExpensesWorkspace(
      Runnable onQueryChange,
      Runnable onAddExpense,
      Runnable onAddCategory,
      Consumer<ExpenseView> onEdit,
      Consumer<ExpenseView> onPay,
      Consumer<ExpenseView> onReminder,
      Consumer<ExpenseView> onDelete,
      Consumer<String> onRequireSelectionError) {
    this.onQueryChange = onQueryChange;

    root.getStyleClass().add("workspace-body");
    root.setMinSize(0, 0);

    searchField.setPromptText("Search expense names…");
    searchField.setPrefWidth(210);
    searchField.setOnAction(
        e -> {
          page = 0;
          this.onQueryChange.run();
        });

    HBox searchBox =
        new HBox(8, DesktopIcons.icon(DesktopIcons.SEARCH, 15), searchField);
    searchBox.setAlignment(Pos.CENTER_LEFT);
    searchBox.getStyleClass().add("search-box");

    List<ExpenseKind> kinds = new ArrayList<>();
    kinds.add(null);
    kinds.addAll(List.of(ExpenseKind.values()));
    kindFilter.setItems(FXCollections.observableArrayList(kinds));
    kindFilter.setPromptText("All kinds");
    kindFilter.setConverter(
        new StringConverter<>() {
          @Override
          public String toString(ExpenseKind k) {
            return k == null ? "All kinds" : DesktopFormat.titleCase(k) + "s";
          }

          @Override
          public ExpenseKind fromString(String s) {
            return null;
          }
        });
    kindFilter.setOnAction(
        e -> {
          if (!suppressEvents) {
            page = 0;
            this.onQueryChange.run();
          }
        });

    List<ExpenseStatus> statuses = new ArrayList<>();
    statuses.add(null);
    statuses.addAll(List.of(ExpenseStatus.values()));
    statusFilter.setItems(FXCollections.observableArrayList(statuses));
    statusFilter.setPromptText("All statuses");
    statusFilter.setConverter(
        new StringConverter<>() {
          @Override
          public String toString(ExpenseStatus s) {
            return s == null ? "All statuses" : DesktopFormat.titleCase(s);
          }

          @Override
          public ExpenseStatus fromString(String s) {
            return null;
          }
        });
    statusFilter.setOnAction(
        e -> {
          if (!suppressEvents) {
            page = 0;
            this.onQueryChange.run();
          }
        });

    categoryFilter.setPromptText("All categories");
    categoryFilter.setConverter(
        new StringConverter<>() {
          @Override
          public String toString(CategoryView c) {
            return c == null ? "All categories" : c.name();
          }

          @Override
          public CategoryView fromString(String s) {
            return null;
          }
        });
    categoryFilter.setOnAction(
        e -> {
          if (!suppressEvents) {
            page = 0;
            this.onQueryChange.run();
          }
        });

    sortSelector.setItems(
        FXCollections.observableArrayList("nextDueDate", "name", "amount"));
    sortSelector.setValue("nextDueDate");
    sortSelector.setConverter(
        new StringConverter<>() {
          @Override
          public String toString(String s) {
            if ("name".equals(s)) return "Sort: Name";
            if ("amount".equals(s)) return "Sort: Amount";
            return "Sort: Due date";
          }

          @Override
          public String fromString(String s) {
            return "nextDueDate";
          }
        });
    sortSelector.setOnAction(
        e -> {
          if (!suppressEvents) {
            page = 0;
            this.onQueryChange.run();
          }
        });

    FlowPane filters =
        new FlowPane(
            8,
            8,
            searchBox,
            kindFilter,
            statusFilter,
            categoryFilter,
            sortSelector,
            DesktopComponents.secondaryButton(
                "Search",
                null,
                () -> {
                  page = 0;
                  this.onQueryChange.run();
                }),
            DesktopComponents.subtleButton(
                "Clear",
                null,
                () -> {
                  suppressEvents = true;
                  searchField.clear();
                  kindFilter.setValue(null);
                  statusFilter.setValue(null);
                  categoryFilter.setValue(null);
                  sortSelector.setValue("nextDueDate");
                  suppressEvents = false;
                  page = 0;
                  this.onQueryChange.run();
                }),
            DesktopComponents.secondaryButton(
                "Add category", DesktopIcons.PLUS, onAddCategory));
    filters.getStyleClass().add("filter-bar");
    filters.setAlignment(Pos.CENTER_LEFT);

    DesktopComponents.configureExpenseTable(
        table, false, onEdit, onPay, onReminder, onDelete, onAddExpense);
    table.setMinHeight(200);
    VBox.setVgrow(table, Priority.ALWAYS);

    prevButton =
        DesktopComponents.secondaryButton(
            "Previous",
            DesktopIcons.CHEVRON_LEFT,
            () -> {
              if (page > 0) {
                page--;
                this.onQueryChange.run();
              }
            });
    nextButton =
        DesktopComponents.secondaryButton(
            "Next",
            DesktopIcons.CHEVRON_RIGHT,
            () -> {
              if (page + 1 < totalPages) {
                page++;
                this.onQueryChange.run();
              }
            });
    pageInfo.getStyleClass().add("pagination-label");

    FlowPane footerActions =
        new FlowPane(
            8,
            8,
            DesktopComponents.secondaryButton(
                "Edit selected",
                DesktopIcons.EDIT,
                () -> withSelected(onEdit, onRequireSelectionError)),
            DesktopComponents.primaryButton(
                "Record payment",
                DesktopIcons.CHECK,
                () -> withSelected(onPay, onRequireSelectionError)),
            DesktopComponents.secondaryButton(
                "Reminder",
                DesktopIcons.BELL,
                () -> withSelected(onReminder, onRequireSelectionError)),
            DesktopComponents.dangerButton(
                "Delete",
                DesktopIcons.TRASH,
                () -> withSelected(onDelete, onRequireSelectionError)),
            prevButton,
            pageInfo,
            nextButton);
    footerActions.getStyleClass().add("table-footer");
    footerActions.setAlignment(Pos.CENTER_LEFT);

    BorderPane panelContent = new BorderPane(table, filters, null, footerActions, null);
    panelContent.getStyleClass().add("panel");
    panelContent.setMinSize(0, 0);
    VBox.setVgrow(panelContent, Priority.ALWAYS);

    root.getChildren().add(panelContent);
  }

  Node node() {
    return root;
  }

  String searchQuery() {
    return searchField.getText();
  }

  ExpenseKind selectedKind() {
    return kindFilter.getValue();
  }

  void setKindPreset(ExpenseKind kind) {
    suppressEvents = true;
    kindFilter.setValue(kind);
    page = 0;
    suppressEvents = false;
  }

  ExpenseStatus selectedStatus() {
    return statusFilter.getValue();
  }

  Long selectedCategoryId() {
    CategoryView c = categoryFilter.getValue();
    return c == null ? null : c.id();
  }

  String selectedSort() {
    return sortSelector.getValue() == null ? "nextDueDate" : sortSelector.getValue();
  }

  int currentPage() {
    return page;
  }

  void update(Page<ExpenseView> expensePage, List<CategoryView> categories) {
    suppressEvents = true;
    CategoryView currentCat = categoryFilter.getValue();
    List<CategoryView> catOptions = new ArrayList<>();
    catOptions.add(null);
    catOptions.addAll(categories);
    categoryFilter.setItems(FXCollections.observableArrayList(catOptions));
    if (currentCat != null) {
      categoryFilter.setValue(
          categories.stream()
              .filter(c -> Objects.equals(c.id(), currentCat.id()))
              .findFirst()
              .orElse(null));
    } else {
      categoryFilter.setValue(null);
    }
    suppressEvents = false;

    table.getItems().setAll(expensePage.content());
    totalPages = Math.max(1, expensePage.totalPages());
    pageInfo.setText(
        "Page "
            + (page + 1)
            + " of "
            + totalPages
            + " · "
            + expensePage.totalElements()
            + (expensePage.totalElements() == 1 ? " expense" : " expenses"));
    prevButton.setDisable(page <= 0);
    nextButton.setDisable(page + 1 >= totalPages);
  }

  private void withSelected(
      Consumer<ExpenseView> action, Consumer<String> onRequireSelectionError) {
    ExpenseView selected = table.getSelectionModel().getSelectedItem();
    if (selected == null) {
      onRequireSelectionError.accept("Select an expense row in the table first.");
    } else {
      action.accept(selected);
    }
  }
}
