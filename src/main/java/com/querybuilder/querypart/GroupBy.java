package com.querybuilder.querypart;

import com.querybuilder.controllers.MainController;
import com.querybuilder.domain.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import net.sf.jsqlparser.statement.select.GroupByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;

public class GroupBy {

    public static void load(MainController controller, PlainSelect pSelect) {
        TreeTableView<TableRow> groupFieldsTree = controller.getGroupFieldsTree();
        TableView<TableRow> groupTableResults = controller.getGroupTableResults();

        GroupByElement groupBy = pSelect.getGroupBy();
        if (groupBy != null) {
            groupBy.getGroupByExpressions().forEach(x -> {
                for (TreeItem<TableRow> ddd : groupFieldsTree.getRoot().getChildren()) {
                    if (ddd.getValue().getName().equals(x.toString())) {
                        controller.makeSelect(ddd, groupFieldsTree, groupTableResults, null);
                        return;
                    }
                }
                TableRow tableRow = new TableRow(x.toString());
                groupTableResults.getItems().add(0, tableRow);
            });
        }
    }


    public void save() {

    }
}
