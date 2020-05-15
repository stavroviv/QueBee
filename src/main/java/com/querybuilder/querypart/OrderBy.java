package com.querybuilder.querypart;

import com.querybuilder.controllers.MainController;
import com.querybuilder.domain.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;

import java.util.List;

public class OrderBy {

    public static void load(MainController controller, PlainSelect pSelect) {
        TableView<TableRow> orderTableResults = controller.getOrderTableResults();
        TreeTableView<TableRow> orderFieldsTree = controller.getOrderFieldsTree();
        List<OrderByElement> orderByElements = pSelect.getOrderByElements();
        if (orderByElements == null) {
            return;
        }
        orderByElements.forEach(x -> {
            boolean selected = false;
            for (TreeItem<TableRow> ddd : orderFieldsTree.getRoot().getChildren()) {
                if (ddd.getValue().getName().equals(x.getExpression().toString())) {
                    controller.makeSelect(ddd, orderFieldsTree, orderTableResults, x.isAsc() ? "Ascending" : "Descending");
                    selected = true;
                    break;
                }
            }
            if (!selected) {
                TableRow tableRow = new TableRow(x.getExpression().toString());
                tableRow.setComboBoxValue(x.isAsc() ? "Ascending" : "Descending");
                orderTableResults.getItems().add(tableRow);
            }
        });
    }
}
