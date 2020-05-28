package com.querybuilder.domain;

import javafx.collections.ListChangeListener;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;

import static com.querybuilder.utils.Constants.ALL_FIELDS;
import static com.querybuilder.utils.Constants.DATABASE_TABLE_ROOT;
import static com.querybuilder.utils.Utils.addElementBeforeTree;

public class SelectedFieldsTree extends TreeItem<TableRow> {
    TreeItem<TableRow> allFieldsRoot;
    TreeTableView<TableRow> tree;

    public SelectedFieldsTree(TreeTableView<TableRow> tablesView,
                              TreeTableView<TableRow> tree,
                              TableView<TableRow> fieldTable) {
        super(new TableRow(DATABASE_TABLE_ROOT));
        this.tree = tree;

        fieldTable.getItems().forEach(x -> {
            TableRow tableRow = TableRow.tableRowFromValue(x);
            TreeItem<TableRow> tableRowTreeItem = new TreeItem<>(tableRow);
            this.getChildren().add(tableRowTreeItem);
        });

        TableRow allFieldsRootRow = new TableRow(ALL_FIELDS);
        allFieldsRoot = new TreeItem<>(allFieldsRootRow);
        this.getChildren().add(allFieldsRoot);

        tablesView.getRoot().getChildren().forEach(x -> {
            TreeItem<TableRow> tableRowTreeItem = newTreeItem(x);
            allFieldsRoot.getChildren().add(tableRowTreeItem);
            x.getChildren().forEach(
                    treeItem -> tableRowTreeItem.getChildren().add(newTreeItem(treeItem))
            );
        });

        tree.setRoot(this);
    }

    public SelectedFieldsTree(TreeTableView<TableRow> tablesView, TreeTableView<TableRow> tree) {
        super(new TableRow(DATABASE_TABLE_ROOT));
        this.tree = tree;

        tablesView.getRoot().getChildren().forEach(x -> {
            TreeItem<TableRow> tableRowTreeItem = newTreeItem(x);
            this.getChildren().add(tableRowTreeItem);
            x.getChildren().forEach(
                    treeItem -> tableRowTreeItem.getChildren().add(newTreeItem(treeItem))
            );
        });

        tree.setRoot(this);
    }

    private TreeItem<TableRow> newTreeItem(TreeItem<TableRow> treeItem) {
        TableRow tableRow = TableRow.tableRowFromValue(treeItem.getValue());
        return new TreeItem<>(tableRow);
    }

    public void applyChanges(ListChangeListener.Change<? extends TreeItem<TableRow>> change) {
        TreeItem<TableRow> root = allFieldsRoot != null ? allFieldsRoot : this;
        if (change.wasAdded()) {
            change.getAddedSubList().forEach(x -> {
                TreeItem<TableRow> tableRowTreeItem = newTreeItem(x);
                root.getChildren().add(tableRowTreeItem);
                x.getChildren().forEach(
                        treeItem -> tableRowTreeItem.getChildren().add(newTreeItem(treeItem))
                );
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

    public void applyChangesString(ListChangeListener.Change<? extends TableRow> change) {
        TreeItem<TableRow> root = this;
        if (change.wasReplaced()) {
            // getRemoved - там значение до изменения
            TableRow tableRow = change.getRemoved().get(0);
            long idChange = tableRow.getId();
            change.getAddedSubList().forEach(x -> {
                for (TreeItem<TableRow> item : root.getChildren()) {
                    if (item.getValue().getId() == idChange) {
                        item.getValue().setName(x.getName());
                        break;
                    }
                }
            });
            this.tree.refresh();
        } else if (change.wasAdded()) {
            change.getAddedSubList().forEach(x -> {
                TreeItem<TableRow> tableRowTreeItem = new TreeItem<>(TableRow.tableRowFromValue(x));
                addElementBeforeTree(root.getChildren(), tableRowTreeItem);
            });
        } else if (change.wasRemoved()) {
            change.getRemoved().forEach(x -> {
                TreeItem<TableRow> deleted = null;
                for (TreeItem<TableRow> item : root.getChildren()) {
                    if (item.getValue().getId() == x.getId()) {
                        deleted = item;
                        break;
                    }
                }
                root.getChildren().remove(deleted);
            });
        }
    }

}