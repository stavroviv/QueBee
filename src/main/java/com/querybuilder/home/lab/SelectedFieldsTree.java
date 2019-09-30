package com.querybuilder.home.lab;

import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;

public class SelectedFieldsTree extends TreeTableView<TableRow> {
    private TreeItem<TableRow> selectedTreeRoot;

    public SelectedFieldsTree(TreeTableView<TableRow> tablesView, TableView<String> fieldTable) {
        TableRow tablesRoot = new TableRow("Tables");
        tablesRoot.setRoot(true);
        TreeItem<TableRow> root = new TreeItem<>(tablesRoot);
//        selectedTreeRoot.getChildren().add(root);
        fieldTable.getItems().forEach(x -> {
            TreeItem<TableRow> tableRowTreeItem = new TreeItem<>(new TableRow(x));
            root.getChildren().add(tableRowTreeItem);
        });

        root.getChildren().add( tablesView.getRoot().getChildren().get(0));
//        root.setExpanded(true);
//        this.setRoot(tablesRoot);
//        selectedTreeRoot.getChildren().add();
        selectedTreeRoot = root;
    }

    public TreeItem<TableRow> getTree() {
        return selectedTreeRoot;
    }
}
