package com.querybuilder.querypart;

import com.querybuilder.controllers.MainController;
import com.querybuilder.domain.SelectedFieldsTree;
import com.querybuilder.domain.TableRow;
import com.querybuilder.eventbus.CustomEvent;
import com.querybuilder.eventbus.CustomEventBus;
import com.querybuilder.utils.CustomCell;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SubSelect;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.querybuilder.domain.ConditionCell.REFRESH_SELECTED_TREE;
import static com.querybuilder.utils.Constants.*;
import static com.querybuilder.utils.Utils.*;

public class FromTables {

    public static void init(MainController controller) {
        setCellsFactories(controller);
        setListeners(controller);
        setCellFactory(controller.getDatabaseTableColumn());
    }

    private static void setListeners(MainController controller) {
        controller.getDatabaseTableView().setOnMousePressed(e -> {
            if (!doubleClick(e)) {
                return;
            }
            TreeItem<TableRow> selectedItem = controller.getDatabaseTableView().getSelectionModel().getSelectedItem();
            String parent = selectedItem.getParent().getValue().getName();
            String field = selectedItem.getValue().getName();
            if (DATABASE_TABLE_ROOT.equals(parent) || CTE_ROOT.equals(parent)) {
                addTablesRow(controller, field);
            } else {
                addTablesRow(controller, parent);
                controller.addFieldRow(parent + "." + field);
            }
        });

        controller.getTablesView().getRoot().getChildren().addListener((ListChangeListener<TreeItem<TableRow>>) change -> {
            while (change.next()) {
                List<SelectedFieldsTree> selectedFieldTrees = new ArrayList<>();
                selectedFieldTrees.add(controller.getSelectedGroupFieldsTree());
                selectedFieldTrees.add(controller.getSelectedConditionsTreeTable());
                selectedFieldTrees.add(controller.getSelectedOrderFieldsTree());
                applyChange(selectedFieldTrees, selectedFieldsTree -> selectedFieldsTree.applyChanges(change));

                CustomEvent customEvent = new CustomEvent();
                customEvent.setName(REFRESH_SELECTED_TREE);
                customEvent.setChange(change);
                CustomEventBus.post(customEvent);
            }
        });
        controller.getTablesView().setOnMousePressed(e -> {
            if (!doubleClick(e)) {
                return;
            }
            TreeItem<TableRow> selectedItem = controller.getTablesView().getSelectionModel().getSelectedItem();
            String parent = selectedItem.getParent().getValue().getName();
            String field = selectedItem.getValue().getName();
            if (!TABLES_ROOT.equals(parent)) {
                controller.addFieldRow(parent + "." + field);
            }
        });

        controller.getFieldTable().getItems().addListener((ListChangeListener<TableRow>) change -> {
            while (change.next()) {
                List<SelectedFieldsTree> selectedFieldTrees = new ArrayList<>();
                selectedFieldTrees.add(controller.getSelectedGroupFieldsTree());
                selectedFieldTrees.add(controller.getSelectedOrderFieldsTree());
                applyChange(selectedFieldTrees, selectedFieldsTree -> selectedFieldsTree.applyChangesString(change));
            }
        });
    }

    private static void applyChange(List<SelectedFieldsTree> fieldsTree, Consumer<SelectedFieldsTree> consumer) {
        fieldsTree.stream().filter(Objects::nonNull).forEach(consumer);
    }

    private static void setCellsFactories(MainController controller) {
        setCellFactory(controller.getTablesViewColumn());
        controller.getTablesViewColumn().setCellFactory(ttc -> new CustomCell() {
            @Override
            protected void updateItem(TableRow item, boolean empty) {
                super.updateItem(item, empty);
                setItem(this, item, empty);
                setContextMenu(tableViewGetContextMenu(controller, item, empty));
            }
        });
    }

    private static ContextMenu tableViewGetContextMenu(MainController controller, TableRow item, boolean empty) {
        MenuItem addContext = new MenuItem("Add");
        MenuItem changeContext = new MenuItem("Change");
        MenuItem deleteContext = new MenuItem("Delete");
        MenuItem renameContext = new MenuItem("Rename");

        addContext.setOnAction((ActionEvent event) -> {
//            System.out.println("addContext");
//            Object item = tablesView.getSelectionModel().getSelectedItem();
//            System.out.println("Selected item: " + item);
        });
        deleteContext.setOnAction((ActionEvent event) -> controller.deleteTableFromSelected());
        renameContext.setOnAction((ActionEvent event) -> {
//            System.out.println("renameContext");
//            Object item = tablesView.getSelectionModel().getSelectedItem();
//            System.out.println("Selected item: " + item);
        });
        changeContext.setOnAction((ActionEvent event) -> {
//            System.out.println("changeContext");
//            TableRow item = tablesView.getSelectionModel().getSelectedItem().getValue();
            controller.openNestedQuery(item.getQuery(), item);
        });

        ContextMenu menu = new ContextMenu();
        menu.getItems().add(addContext);
        if (!empty && item.isRoot()) {
            if (item.isNested()) {
                menu.getItems().add(changeContext);
            }
            menu.getItems().add(deleteContext);
            menu.getItems().add(renameContext);
        }
        return menu;
    }

    private static void addTablesRow(MainController controller, String parent) {
        ObservableList<TreeItem<TableRow>> children = controller.getTablesView().getRoot().getChildren();
        if (children.stream().noneMatch(x -> x.getValue().getName().equals(parent))) {
            controller.getTablesView().getRoot().getChildren().add(
                    getTableItemWithFields(controller, parent)
            );
        }
        controller.refreshLinkTable();
    }

    private static TreeItem<TableRow> getTableItemWithFields(MainController controller, String tableName) {
        TableRow tableRoot = new TableRow(tableName);
        tableRoot.setRoot(true);

        AtomicReference<Boolean> cte = new AtomicReference<>(false);
        ObservableList<String> columns = getColumns(controller, tableName, cte);
        TreeItem<TableRow> treeItem = new TreeItem<>(tableRoot);
        columns.forEach(col -> {
            TableRow tableRow = new TableRow(col);
            TreeItem<TableRow> tableRowTreeItem = new TreeItem<>(tableRow);
            treeItem.getChildren().add(tableRowTreeItem);
        });
        treeItem.getValue().setCte(cte.get());

        return treeItem;
    }

    public static void load(MainController controller, PlainSelect pSelect) {
        TreeTableView<TableRow> tablesView = controller.getTablesView();
        FromItem fromItem = pSelect.getFromItem();
        Table table = null;
        if (fromItem instanceof Table) {
            table = (Table) fromItem;
            tablesView.getRoot().getChildren().add(getTableItemWithFields(controller, table.getName()));
        }
        List<Join> joins = pSelect.getJoins();
        if (joins == null || table == null) {
            return;
        }

        for (Join join : joins) {
            FromItem rightItem = join.getRightItem();
            String rightItemName = "";
            if (rightItem instanceof Table) {
                rightItemName = rightItem.toString();
                tablesView.getRoot().getChildren().add(getTableItemWithFields(controller, rightItemName));
            } else if (rightItem instanceof SubSelect) {
                SubSelect sSelect = (SubSelect) rightItem;
                rightItemName = sSelect.getAlias().getName();
                TableRow tableRow = new TableRow(rightItemName);
                tableRow.setNested(true);
                tableRow.setRoot(true);
                String queryText = sSelect.toString().replace(sSelect.getAlias().toString(), "");
                queryText = queryText.substring(1, queryText.length() - 1);
                tableRow.setQuery(queryText);
                TreeItem<TableRow> tableRowTreeItem = new TreeItem<>(tableRow);
                tablesView.getRoot().getChildren().add(tableRowTreeItem);

                PlainSelect plainSelect = (PlainSelect) sSelect.getSelectBody();
                plainSelect.getSelectItems().forEach((sItem) -> {
                    TableRow nestedItem = new TableRow(sItem.toString());
                    TreeItem<TableRow> nestedRow = new TreeItem<>(nestedItem);
                    tableRowTreeItem.getChildren().add(nestedRow);
                });
            }
        }
    }

    public static void save(MainController controller, PlainSelect selectBody) throws Exception {
        if (!controller.getLinkTable().getItems().isEmpty()) {
            return;
        }
        List<Join> joins = new ArrayList<>();
        controller.getTablesView().getRoot().getChildren().forEach(x -> {
            String tableName = x.getValue().getName();
            if (selectBody.getFromItem() == null) {
                selectBody.setFromItem(new Table(tableName));
            } else {
                Join join = new Join();
                join.setRightItem(new Table(tableName));
                join.setSimple(true);
                joins.add(join);
            }
        });
        selectBody.setJoins(joins);
    }

}
