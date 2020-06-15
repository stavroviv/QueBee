package com.querybuilder.utils;

import com.querybuilder.controllers.MainController;
import com.querybuilder.domain.AliasRow;
import com.querybuilder.domain.CteRow;
import com.querybuilder.domain.SelectedFieldsTree;
import com.querybuilder.domain.TableRow;
import com.querybuilder.domain.qparts.OneCte;
import com.querybuilder.domain.qparts.Orderable;
import com.querybuilder.domain.qparts.Union;
import com.querybuilder.eventbus.Subscriber;
import com.querybuilder.querypart.FromTables;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.querybuilder.utils.Constants.*;

public class Utils {

    public static void showErrorMessage(Exception exception, String header) {
        JOptionPane.showMessageDialog(
                null,
                getMessage(exception),
                header,
                JOptionPane.ERROR_MESSAGE
        );
        exception.printStackTrace();
    }

    public static void showErrorMessage(Exception exception) {
        showErrorMessage(exception, "Parse query error");
    }

    public static void showErrorMessage(String text, String header) {
        JOptionPane.showMessageDialog(
                null,
                text,
                header,
                JOptionPane.ERROR_MESSAGE
        );
    }

    private static String getMessage(Exception exception) {
        StringBuilder result = new StringBuilder();
        if (exception instanceof JSQLParserException) {
            String message = exception.getCause().getMessage();
            String[] split = message.split("\n");

            int i = 0;
            boolean found = false;
            for (String s : split) {
                result.append(s.trim());
                if (s.contains("Was expect") || i % 7 == 1) {
                    result.append("\n");
                    found = true;
                } else {
                    result.append(" ");
                }
                if (found) {
                    i++;
                }
            }
        } else {
            result.append(exception.getMessage());
        }

        return result.toString();
    }

    public static void setEmptyHeader(Control control) {
        control.widthProperty().addListener((ov, t, t1) -> {
            Pane header = (Pane) control.lookup("TableHeaderRow");
            if (header != null && header.isVisible()) {
                header.setMaxHeight(0);
                header.setMinHeight(0);
                header.setPrefHeight(0);
                header.setVisible(false);
                header.setManaged(false);
            }
        });
    }

    public static void setDefaultSkin(PopupControl popup, Control control, Control cell) {
        popup.setSkin(new Skin<Skinnable>() {
            @Override
            public Skinnable getSkinnable() {
                return null;
            }

            @Override
            public Node getNode() {
                control.setMinWidth(cell.getWidth());
                control.setMaxWidth(cell.getWidth());
                return control;
            }

            @Override
            public void dispose() {
            }
        });
    }

    public static void setCellFactory(TreeTableColumn<com.querybuilder.domain.TableRow, TableRow> tablesViewColumn, FromTables controller) {
        tablesViewColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getValue()));
        tablesViewColumn.setCellFactory(ttc -> new CustomCell(controller));
    }

    public static void setCellFactory(TreeTableColumn<com.querybuilder.domain.TableRow, TableRow> tablesViewColumn) {
        setCellFactory(tablesViewColumn, null);
    }

    public static ObservableList<String> getColumns(MainController controller, String table, AtomicReference<Boolean> isCte) {
        List<String> resultColumns = new ArrayList<>();
        List<String> dbColumns = controller.getDbElements().get(table);
        if (dbColumns != null) {
            resultColumns.addAll(dbColumns);
            return FXCollections.observableArrayList(resultColumns);
        }

        // CTE
        TreeTableView<TableRow> databaseTableView = controller.getTableFieldsController().getDatabaseTableView();
        ObservableList<TreeItem<TableRow>> tables = databaseTableView.getRoot().getChildren();
        if (tables.size() > 0 && tables.get(0).getValue().getName().equals(CTE_ROOT)) {
            ObservableList<TreeItem<TableRow>> cte = tables.get(0).getChildren();
            for (TreeItem<TableRow> item : cte) {
                if (!item.getValue().getName().equals(table)) {
                    continue;
                }
                isCte.set(true);
                item.getChildren().forEach(col -> resultColumns.add(col.getValue().getName()));
                break;
            }
        }
        return FXCollections.observableArrayList(resultColumns);
    }

    public static void openForm(String formName, String title, Map<String, Object> userData) {
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.initModality(Modality.APPLICATION_MODAL);
        try {
            stage.setScene(getScene(formName, userData));
        } catch (Exception e) {
            showErrorMessage(e);
            return;
        }
        stage.setOnCloseRequest((e) -> {
            System.out.println("sdfsdf");
        });
        stage.show();
    }

    public static Scene getScene(String formName, Map<String, Object> userData) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(Utils.class.getResource(formName));
        FXMLLoader.setDefaultClassLoader(Utils.class.getClassLoader());
        Parent root = fxmlLoader.load();
        Subscriber controller = fxmlLoader.getController();
        controller.initData(userData);
        return new Scene(root);
    }

    public static boolean doubleClick(MouseEvent e) {
        return e.getClickCount() == 2 && e.isPrimaryButtonDown();
    }

    public static <E> void addElementBeforeTree(ObservableList<E> list, E row) {
        // try {
        if (list.isEmpty()) {
            list.add(row);
        } else {
            list.add(list.size() - 1, row);
        }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    public static void applyChange(List<SelectedFieldsTree> fieldsTree, Consumer<SelectedFieldsTree> consumer) {
        fieldsTree.stream().filter(Objects::nonNull).forEach(consumer);
    }

    public static String getNameFromColumn(Column column) {
        Table table = column.getTable();
        if (table == null) {
            throw new RuntimeException("Ambiguous column reference: " + column);
        }
        return table.getName() + "." + column.getColumnName();
    }

    public static void setCellSelectionEnabled(TableView<TableRow> table) {
        table.getSelectionModel().setCellSelectionEnabled(true);
    }

    public static void setStringColumnFactory(TableColumn<TableRow, String> resultsColumn) {
        setStringColumnFactory(resultsColumn, false);
    }

    public static void setStringColumnFactory(TableColumn<TableRow, String> resultsColumn, boolean editable) {
        resultsColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        resultsColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        resultsColumn.setEditable(editable);
    }

    public static void setComboBoxColumnFactory(TableColumn<TableRow, String> column, String... items) {
        column.setEditable(true);
        column.setCellFactory(
                ComboBoxTableCell.forTableColumn(FXCollections.observableArrayList(items))
        );
        column.setCellValueFactory(cellData -> cellData.getValue().comboBoxValueProperty());
    }

    public static void setResultsTableSelectHandler(TableView<TableRow> groupTableResults, TreeTableView<TableRow> groupFieldsTree) {
        groupTableResults.setOnMousePressed(e -> {
            if (!doubleClick(e)) {
                return;
            }
            makeDeselect(groupTableResults, groupFieldsTree);
        });
    }

    // DESELECT

    public static void makeDeselectAll(TableView<TableRow> resultsTable, TreeTableView<TableRow> fieldsTree) {
        for (TableRow item : resultsTable.getItems()) {
            TreeItem<TableRow> treeItem = new TreeItem<>(item);
            addElementBeforeTree(fieldsTree.getRoot().getChildren(), treeItem);
        }
        resultsTable.getItems().clear();
    }

    private static final List<String> notSelectColumns = Arrays.asList("groupTableAggregates", "orderTableResults");

    public static void makeDeselect(TableView<TableRow> resultsTable, TreeTableView<TableRow> fieldsTree, TableRow item) {
        if (item == null) {
            item = resultsTable.getSelectionModel().getSelectedItem();
        }

        ObservableList<TablePosition> selectedCells = resultsTable.getSelectionModel().getSelectedCells();
        if (notSelectColumns.contains(resultsTable.getId())
                && !selectedCells.isEmpty() && selectedCells.get(0).getColumn() == 1) {
            return;
        }
        TableRow tableRow = new TableRow(item.getName());
        if (tableRow.isNotSelectable()) {
            return;
        }
        TreeItem<TableRow> treeItem = new TreeItem<>(tableRow);
        addElementBeforeTree(fieldsTree.getRoot().getChildren(), treeItem);
        resultsTable.getItems().remove(item);
    }

    public static void makeDeselect(TableView<TableRow> resultsTable, TreeTableView<TableRow> fieldsTree) {
        makeDeselect(resultsTable, fieldsTree, null);
    }

    // SELECT
    public static void makeSelectAll(TreeTableView<TableRow> fieldsTree, TableView<TableRow> resultsTable, String value) {
        List<TreeItem<TableRow>> deleteItems = new ArrayList<>();
        for (TreeItem<TableRow> item : fieldsTree.getRoot().getChildren()) {
            if (item.getValue().getName().equals(ALL_FIELDS)) {
                break;
            }
            deleteItems.add(item);
            TableRow tableRow = TableRow.tableRowFromValue(item.getValue());
            if (value != null) {
                tableRow.setComboBoxValue(value);
            }
            resultsTable.getItems().add(tableRow);
        }
        for (TreeItem<TableRow> item : deleteItems) {
            fieldsTree.getRoot().getChildren().remove(item);
        }
    }

    public static void makeSelectAll(TreeTableView<TableRow> fieldsTree, TableView<TableRow> resultsTable) {
        makeSelectAll(fieldsTree, resultsTable, "");
    }

    public static void makeSelect(TreeTableView<TableRow> fieldsTree, TableView<TableRow> table) {
        makeSelect(fieldsTree, table, null);
    }

    public static void makeSelect(TreeTableView<TableRow> fieldsTree, TableView<TableRow> table, String value) {
        makeSelect(fieldsTree, table, null, value);
    }

    public static void makeSelect(TreeTableView<TableRow> fieldsTree, TableView<TableRow> table,
                                  TreeItem<TableRow> item, String value) {
        if (item == null) {
            item = fieldsTree.getSelectionModel().getSelectedItem();
        }

        if (item.getChildren().size() > 0) {
            return;
        }
        String name = item.getValue().getName();

        TreeItem<TableRow> parent = item.getParent();
        String parentName = parent.getValue().getName();
        TableRow newItem;
        if (!parentName.equals(DATABASE_TABLE_ROOT)) {
            name = parentName + "." + name;
            newItem = new TableRow(name);
        } else {
            newItem = TableRow.tableRowFromValue(item.getValue());
        }

        if (value != null) {
            newItem.setComboBoxValue(value);
        }
        table.getItems().add(newItem);
        fieldsTree.getRoot().getChildren().remove(item);
    }

    public static void setTreeSelectHandler(TreeTableView<TableRow> fieldsTree, TableView<TableRow> table) {
        setTreeSelectHandler(fieldsTree, table, "");
    }

    public static void setTreeSelectHandler(TreeTableView<TableRow> tree, TableView<TableRow> table, String value) {
        tree.setOnMousePressed(e -> {
            if (doubleClick(e)) {
                makeSelect(tree, table, value);
            }
        });
    }

    public static void activateNewTab(Tab newTab, TabPane tabPane, MainController controller) {
        SingleSelectionModel<Tab> selectionModel = tabPane.getSelectionModel();
        selectionModel.select(newTab);
        SingleSelectionModel<Tab> selModel = controller.getMainTabPane().getSelectionModel();
        selModel.select(controller.getTableAndFieldsTab());
    }

    public static void removeEmptyAliases(MainController controller) {
        ObservableList<AliasRow> aliasItems = controller.getUnionAliasesController().getAliasTable().getItems();
        List<AliasRow> delList = new ArrayList<>();
        for (AliasRow item : aliasItems) {
            boolean allEmpty = true;
            for (Map.Entry<String, String> stringStringEntry : item.getValues().entrySet()) {
                if (!stringStringEntry.getValue().equals(EMPTY_UNION_VALUE)) {
                    allEmpty = false;
                    break;
                }
            }
            if (allEmpty) {
                delList.add(item);
            }
        }
        for (AliasRow aliasRow : delList) {
            controller.getUnionAliasesController().getAliasTable().getItems().remove(aliasRow);
        }
    }

    public static <T> void loadTableToTable(TableView<T> tableFrom, TableView<T> tableTo) {
        tableTo.getItems().clear();
        tableTo.getItems().addAll(tableFrom.getItems());
    }

    public static boolean notEmptyString(String text) {
        return text != null && !text.isEmpty() && !text.trim().isEmpty();
    }

    public static boolean isEmptySearchText(String newValue) {
        return newValue.trim().isEmpty() || newValue.length() < 2;
    }

    public static <T extends Orderable> Map<String, T> sortByOrder(Map<String, T> map) {
        List<Map.Entry<String, T>> list = new LinkedList<>(map.entrySet());

        list.sort(Comparator.comparing(o -> o.getValue().getOrder()));

        Map<String, T> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<String, T> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

    public static void setUpDownBind(TableView<?> aliasTable, Button buttonUp, Button buttonDown) {
        ReadOnlyIntegerProperty selectedIndex = aliasTable.getSelectionModel().selectedIndexProperty();
        buttonUp.disableProperty().bind(selectedIndex.lessThanOrEqualTo(0));
        buttonDown.disableProperty().bind(Bindings.createBooleanBinding(() -> {
                    int index = selectedIndex.get();
                    return index < 0 || index + 1 >= aliasTable.getItems().size();
                },
                selectedIndex, aliasTable.getItems())
        );
    }

    public static <T> void moveRowUp(TableView<T> tableView) {
        moveRow(tableView, -1);
    }

    public static <T> void moveRowDown(TableView<T> tableView) {
        moveRow(tableView, 1);
    }

    public static <T> void moveRow(TableView<T> tableView, int upDown) {
        int index = tableView.getSelectionModel().getSelectedIndex();
        tableView.getItems().add(index + upDown, tableView.getItems().remove(index));
        tableView.getSelectionModel().clearAndSelect(index + upDown);
    }

//    public static void moveTabUp(TabPane tabPane, TableView<TableRow> unionTable) {
//        moveTab(tabPane, unionTable, -1);
//    }
//
//    public static void moveTabDown(TabPane tabPane, TableView<TableRow> unionTable) {
//        moveTab(tabPane, unionTable, 1);
//    }

    public static void moveTab(TabPane tabPane, TableView<TableRow> table, int upDown) {
        TableRow selectedItem = table.getSelectionModel().getSelectedItem();

        int tabIndex = getTabIndex(tabPane, selectedItem.getName());

        Tab remove = tabPane.getTabs().remove(tabIndex);
        tabPane.getTabs().add(tabIndex + upDown, remove);
        tabPane.getSelectionModel().clearAndSelect(tabIndex + upDown);
    }

    public static void moveCteTabUp(TabPane tabPane, TableView<CteRow> table) {
        moveCteTab(tabPane, table, -1);
    }

    public static void moveCteTabDown(TabPane tabPane, TableView<CteRow> table) {
        moveCteTab(tabPane, table, 1);
    }

    private static void moveCteTab(TabPane tabPane, TableView<CteRow> table, int upDown) {
        CteRow selectedItem = table.getSelectionModel().getSelectedItem();

        int tabIndex = getTabIndex(tabPane, selectedItem.getId());

        Tab remove = tabPane.getTabs().remove(tabIndex);
        tabPane.getTabs().add(tabIndex + upDown, remove);
        tabPane.getSelectionModel().clearAndSelect(tabIndex + upDown);
    }

    public static int getTabIndex(TabPane tabPane, String unionTabId) {
        int tIndex = 0;
        for (Tab tPane : tabPane.getTabs()) {
            if (tPane.getId().equals(unionTabId)) {
                break;
            }
            tIndex++;
        }
        return tIndex;
    }

    public static void changeUnionOrder(MainController mainController, TableView<TableRow> unionTable, int upDown) {
        TableRow selectedItem = unionTable.getSelectionModel().getSelectedItem();

        OneCte currentCte = mainController.getCurrentCte();
        Union current = currentCte.getUnionMap().get(selectedItem.getName());

        changeOrderInQueryObjects(sortByOrder(currentCte.getUnionMap()), upDown, current);
    }

    public static void changeCteOrder(MainController mainController, TableView<CteRow> unionTable, int upDown) {
        CteRow selectedItem = unionTable.getSelectionModel().getSelectedItem();
        String id = selectedItem.getId();

        Map<String, OneCte> cteMap = mainController.getFullQuery().getCteMap();
        OneCte current = cteMap.get(id);

        changeOrderInQueryObjects(sortByOrder(cteMap), upDown, current);
    }

    private static void changeOrderInQueryObjects(Map<String, ? extends Orderable> map, int upDown, Orderable current) {
        Orderable neighbour = null;
        boolean foundNext = false;
        Integer currentOrder = current.getOrder();

        for (Map.Entry<String, ? extends Orderable> stringUnionEntry : map.entrySet()) {
            if (stringUnionEntry.getValue().equals(current)) {
                if (upDown == -1) {
                    break;
                } else {
                    foundNext = true;
                    continue;
                }
            }
            neighbour = stringUnionEntry.getValue();
            if (foundNext) {
                break;
            }
        }

        Integer orderPrev = neighbour.getOrder();
        neighbour.setOrder(currentOrder);
        current.setOrder(orderPrev);
    }

}
