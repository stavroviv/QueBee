package com.querybuilder.querypart;

import com.querybuilder.controllers.MainController;
import com.querybuilder.domain.TableRow;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.scene.control.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;

import java.util.ArrayList;
import java.util.List;

import static com.querybuilder.utils.Constants.ORDER_DEFAULT_VALUE;
import static com.querybuilder.utils.Utils.*;

public class OrderBy {

    public static void init(MainController controller) {
        TreeTableView<TableRow> orderFieldsTree = controller.getOrderFieldsTree();
        TableView<TableRow> orderTableResults = controller.getOrderTableResults();
        TableColumn<TableRow, String> orderTableResultsFieldColumn = controller.getOrderTableResultsFieldColumn();
        TableColumn<TableRow, String> orderTableResultsSortingColumn = controller.getOrderTableResultsSortingColumn();

        setTreeSelectHandler(orderFieldsTree, orderTableResults, ORDER_DEFAULT_VALUE);
        setStringColumnFactory(orderTableResultsFieldColumn);

        setComboBoxColumnFactory(orderTableResultsSortingColumn, ORDER_DEFAULT_VALUE, "Descending");
        setResultsTableSelectHandler(orderTableResults, orderFieldsTree);

        // buttons
        Button orderDownButton = controller.getOrderDownButton();
        Button orderUpButton = controller.getOrderUpButton();
        ReadOnlyIntegerProperty selectedIndex = orderTableResults.getSelectionModel().selectedIndexProperty();
        orderUpButton.disableProperty().bind(selectedIndex.lessThanOrEqualTo(0));
        orderDownButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
                    int index = selectedIndex.get();
                    return index < 0 || index + 1 >= orderTableResults.getItems().size();
                },
                selectedIndex, orderTableResults.getItems())
        );

        setCellFactory(controller.getOrderFieldsTreeColumn());
    }

    public static void load(MainController controller, PlainSelect pSelect) {
        List<OrderByElement> orderByElements = pSelect.getOrderByElements();
        if (orderByElements == null) {
            return;
        }
        TableView<TableRow> orderTableResults = controller.getOrderTableResults();
        TreeTableView<TableRow> orderFieldsTree = controller.getOrderFieldsTree();
        orderByElements.forEach(x -> {
            boolean selected = false;
            for (TreeItem<TableRow> ddd : orderFieldsTree.getRoot().getChildren()) {
                if (ddd.getValue().getName().equals(x.getExpression().toString())) {
                    makeSelect(
                            ddd, orderFieldsTree, orderTableResults, x.isAsc() ? "Ascending" : "Descending"
                    );
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

    public static void save(MainController controller, PlainSelect selectBody) {
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
