package com.querybuilder.home.lab;

import javafx.collections.ListChangeListener;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;

import static com.querybuilder.home.lab.Constants.ALL_FIELDS;
import static com.querybuilder.home.lab.Constants.DATABASE_ROOT;

public class SelectedFieldsTree extends TreeItem<TableRow> {
    TreeItem<TableRow> allFieldsRoot;

    public SelectedFieldsTree(TreeTableView<TableRow> tablesView, TableView<String> fieldTable) {
        super(new TableRow(DATABASE_ROOT));
        fieldTable.getItems().forEach(x -> {
            TreeItem<TableRow> tableRowTreeItem = new TreeItem<>(new TableRow(x));
            this.getChildren().add(tableRowTreeItem);
        });
        TableRow allFieldsRootRow = new TableRow(ALL_FIELDS);
        allFieldsRoot = new TreeItem<>(allFieldsRootRow);
        this.getChildren().add(allFieldsRoot);
        tablesView.getRoot().getChildren().forEach(x -> {
            TreeItem<TableRow> tableRowTreeItem = newTreeItem(x, true);
            allFieldsRoot.getChildren().add(tableRowTreeItem);
            x.getChildren().forEach(y -> tableRowTreeItem.getChildren().add((newTreeItem(y))));
        });
    }

    public SelectedFieldsTree(TreeTableView<TableRow> tablesView) {
        super(new TableRow(DATABASE_ROOT));
        tablesView.getRoot().getChildren().forEach(x -> {
            TreeItem<TableRow> tableRowTreeItem = newTreeItem(x, true);
            this.getChildren().add(tableRowTreeItem);
            x.getChildren().forEach(y -> tableRowTreeItem.getChildren().add(newTreeItem(y)));
        });
    }

    private TreeItem<TableRow> newTreeItem(TreeItem<TableRow> x) {
        return newTreeItem(x, false);
    }

    private TreeItem<TableRow> newTreeItem(TreeItem<TableRow> x, boolean isRoot) {
        TableRow tableRow = new TableRow(x.getValue().getName(), isRoot);
        return new TreeItem<>(tableRow);
    }

    public void applyChanges(ListChangeListener.Change<? extends TreeItem<TableRow>> change) {
        TreeItem<TableRow> root = allFieldsRoot != null ? allFieldsRoot : this;
        if (change.wasAdded()) {
            change.getAddedSubList().forEach(x -> {
                TreeItem<TableRow> tableRowTreeItem = newTreeItem(x, true);
                root.getChildren().add(tableRowTreeItem);
                x.getChildren().forEach(y -> tableRowTreeItem.getChildren().add(newTreeItem(y)));
            });
        } else if (change.wasRemoved()) {
            change.getRemoved().forEach(x -> {
                TreeItem<TableRow> deleted = null;
                for (TreeItem<TableRow> item : root.getChildren()) {
                    if (item.getValue().getName().equals(x.getValue().getName())) {
                        deleted = item;
                        break;
                    }
                }
                root.getChildren().remove(deleted);
            });
        }
    }

    public void applyChangesString(ListChangeListener.Change<? extends String> change) {
        TreeItem<TableRow> root = this;
        if (change.wasAdded()) {
            change.getAddedSubList().forEach(x -> {
                TreeItem<TableRow> tableRowTreeItem = new TreeItem<>(new TableRow(x, false));
                root.getChildren().add(0, tableRowTreeItem);
            });
        } else if (change.wasRemoved()) {
            change.getRemoved().forEach(x -> {
                TreeItem<TableRow> deleted = null;
                for (TreeItem<TableRow> item : root.getChildren()) {
                    if (item.getValue().getName().equals(x)) {
                        deleted = item;
                        break;
                    }
                }
                root.getChildren().remove(deleted);
            });
        }
    }

}