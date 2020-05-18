package com.querybuilder.querypart;

import com.querybuilder.controllers.MainController;
import com.querybuilder.domain.TableRow;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SubSelect;

import java.util.ArrayList;
import java.util.List;

public class FromTables {

    public static void load(MainController controller, PlainSelect pSelect) {
        TreeTableView<TableRow> tablesView = controller.getTablesView();
        FromItem fromItem = pSelect.getFromItem();
        Table table = null;
        if (fromItem instanceof Table) {
            table = (Table) fromItem;
            tablesView.getRoot().getChildren().add(controller.getTableItemWithFields(table.getName()));
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
                tablesView.getRoot().getChildren().add(controller.getTableItemWithFields(rightItemName));
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
