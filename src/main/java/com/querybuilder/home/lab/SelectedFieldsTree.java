package com.querybuilder.home.lab;

import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;

public class SelectedFieldsTree extends TreeItem<TableRow> {

    public SelectedFieldsTree(TreeTableView<TableRow> tablesView, TableView<String> fieldTable) {
        super(new TableRow("Tables"));
        fieldTable.getItems().forEach(x -> {
            TreeItem<TableRow> tableRowTreeItem = new TreeItem<>(new TableRow(x));
            this.getChildren().add(tableRowTreeItem);
        });
        TableRow allFieldsRootRow = new TableRow("All fields");
        TreeItem<TableRow> allFieldsRoot = new TreeItem<>(allFieldsRootRow);
        this.getChildren().add(allFieldsRoot);
        tablesView.getRoot().getChildren().forEach(x -> {
            TableRow tableRow = new TableRow(x.getValue().getName(), true);
            TreeItem<TableRow> tableRowTreeItem = new TreeItem<>(tableRow);
            allFieldsRoot.getChildren().add(tableRowTreeItem);
            x.getChildren().forEach(y -> tableRowTreeItem.getChildren().add(new TreeItem<>(new TableRow(y.getValue().getName()))));
        });
    }

    public SelectedFieldsTree(TreeTableView<TableRow> tablesView) {
        super(new TableRow("Tables"));
        tablesView.getRoot().getChildren().forEach(x -> {
            TableRow tableRow = new TableRow(x.getValue().getName(), true);
            TreeItem<TableRow> tableRowTreeItem = new TreeItem<>(tableRow);
            this.getChildren().add(tableRowTreeItem);
            x.getChildren().forEach(y -> tableRowTreeItem.getChildren().add(new TreeItem<>(new TableRow(y.getValue().getName()))));
        });
    }
}