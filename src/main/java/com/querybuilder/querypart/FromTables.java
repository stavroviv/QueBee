package com.querybuilder.querypart;

import com.querybuilder.controllers.MainController;
import com.querybuilder.domain.TableRow;
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

import static com.querybuilder.utils.Constants.DATABASE_ROOT;
import static com.querybuilder.utils.Constants.TABLES_ROOT;
import static com.querybuilder.utils.Utils.doubleClick;
import static com.querybuilder.utils.Utils.setCellFactory;

public class FromTables {

    public static void init(MainController controller) {
        controller.getDatabaseTableView().setOnMousePressed(e -> {
            if (doubleClick(e)) {
                TreeItem<TableRow> selectedItem = controller.getDatabaseTableView().getSelectionModel().getSelectedItem();
                String parent = selectedItem.getParent().getValue().getName();
                String field = selectedItem.getValue().getName();
                if (DATABASE_ROOT.equals(parent)) {
                    addTablesRow(controller, field);
                } else {
                    addTablesRow(controller, parent);
                    controller.addFieldRow(parent + "." + field);
                }
            }
        });
        controller.getTablesView().getRoot().getChildren().addListener(
                (ListChangeListener<TreeItem<TableRow>>) c -> {
                    while (c.next()) {
                        controller.getSelectedGroupFieldsTree().applyChanges(c);
                        controller.getSelectedConditionsTreeTable().applyChanges(c);
//                        selectedConditionsTreeTableContext.applyChanges(c);
                        controller.getSelectedOrderFieldsTree().applyChanges(c);
                    }
                }
        );
        controller.getFieldTable().getItems().addListener(
                (ListChangeListener<TableRow>) c -> {
                    while (c.next()) {
                        controller.getSelectedGroupFieldsTree().applyChangesString(c);
                        controller.getSelectedOrderFieldsTree().applyChangesString(c);
                    }
                }
        );
        controller.getTablesView().setOnMousePressed(e -> {
            if (doubleClick(e)) {
                TreeItem<TableRow> selectedItem = controller.getTablesView().getSelectionModel().getSelectedItem();
                String parent = selectedItem.getParent().getValue().getName();
                String field = selectedItem.getValue().getName();
                if (!TABLES_ROOT.equals(parent)) {
                    controller.addFieldRow(parent + "." + field);
                }
            }
        });
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
            controller.getTablesView().getRoot().getChildren().add(getTableItemWithFields(controller, parent));
        }
        controller.refreshLinkTable();
    }

    private static TreeItem<TableRow> getTableItemWithFields(MainController controller, String tableName) {
        TableRow tableRoot = new TableRow(tableName);
        tableRoot.setRoot(true);
        TreeItem<TableRow> treeItem = new TreeItem<>(tableRoot);
        List<String> columns = controller.getDbElements().get(tableName);
        if (columns != null) {
            columns.forEach(col -> {
                TableRow tableRow = new TableRow(col);
                TreeItem<TableRow> tableRowTreeItem = new TreeItem<>(tableRow);
                treeItem.getChildren().add(tableRowTreeItem);
            });
        }
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
