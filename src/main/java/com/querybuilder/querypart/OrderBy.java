package com.querybuilder.querypart;

import com.querybuilder.controllers.MainController;
import com.querybuilder.domain.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;

import java.util.ArrayList;
import java.util.List;

public class OrderBy implements QueryPart {

    @Override
    public void load(MainController controller, PlainSelect pSelect) {
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

    @Override
    public void save(MainController controller, PlainSelect selectBody) {
        List<OrderByElement> orderElements = new ArrayList<>();
        controller.getOrderTableResults().getItems().forEach(x -> {
            OrderByElement orderByElement = new OrderByElement();
            Column column = new Column(x.getName());
            orderByElement.setExpression(column);
            orderByElement.setAsc(x.getComboBoxValue().equals("Ascending"));
            orderElements.add(orderByElement);
        });
        selectBody.setOrderByElements(orderElements);
    }
}
