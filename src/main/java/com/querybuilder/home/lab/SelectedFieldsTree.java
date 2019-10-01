package com.querybuilder.home.lab;

import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;

public class SelectedFieldsTree {
    private TreeItem<TableRow> selectedTreeRoot;

    public SelectedFieldsTree(TreeTableView<TableRow> tablesView, TableView<String> fieldTable) {
        TableRow tablesRoot = new TableRow("Tables", true);
        TreeItem<TableRow> root = new TreeItem<>(tablesRoot);
        fieldTable.getItems().forEach(x -> {
            TreeItem<TableRow> tableRowTreeItem = new TreeItem<>(new TableRow(x));
            root.getChildren().add(tableRowTreeItem);
        });

        TableRow allFieldsRootRow = new TableRow("All fields");
        TreeItem<TableRow> allFieldsRoot = new TreeItem<>(allFieldsRootRow);
        root.getChildren().add(allFieldsRoot);
        tablesView.getRoot().getChildren().forEach(x -> {
            TableRow tableRow = new TableRow(x.getValue().getName(), true);
            TreeItem<TableRow> tableRowTreeItem = new TreeItem<>(tableRow);
            allFieldsRoot.getChildren().add(tableRowTreeItem);
            x.getChildren().forEach(y -> tableRowTreeItem.getChildren().add(new TreeItem<>(new TableRow(y.getValue().getName()))));
        });

        selectedTreeRoot = root;
    }

    public TreeItem<TableRow> getTree() {
        return selectedTreeRoot;
    }
}
