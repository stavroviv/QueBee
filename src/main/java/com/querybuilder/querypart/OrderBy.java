
package com.querybuilder.querypart;

import com.querybuilder.domain.TableRow;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SetOperationList;

import java.util.List;

import static com.querybuilder.utils.Constants.ORDER_DEFAULT_VALUE;
import static com.querybuilder.utils.Utils.*;

@Data
@EqualsAndHashCode(callSuper = false)
public class OrderBy extends AbstractQueryPart {
    @FXML
    private TreeTableView<TableRow> orderFieldsTree;
    @FXML
    private TreeTableColumn<TableRow, TableRow> orderFieldsTreeColumn;
    @FXML
    private TableView<TableRow> orderTableResults;
    @FXML
    private TableColumn<TableRow, String> orderTableResultsFieldColumn;
    @FXML
    private TableColumn<TableRow, String> orderTableResultsSortingColumn;
    @FXML
    private Button orderUpButton;
    @FXML
    private Button orderDownButton;

    @Override
    public void initialize() {
        setListeners();
        setCellsFactories();
    }

    private void setListeners() {
        setTreeSelectHandler(orderFieldsTree, orderTableResults, ORDER_DEFAULT_VALUE);
        setResultsTableSelectHandler(orderTableResults, orderFieldsTree);
    }

    private void setCellsFactories() {
        setStringColumnFactory(orderTableResultsFieldColumn);

        setComboBoxColumnFactory(orderTableResultsSortingColumn, ORDER_DEFAULT_VALUE, "Descending");
        setUpDownBind(orderTableResults, orderUpButton, orderDownButton);

        setCellFactory(orderFieldsTreeColumn);
    }

    public TableView<TableRow> load(SelectBody selectBody) {
        TableView<TableRow> orderTableResults = new TableView<>();
        if (selectBody == null) {
            return orderTableResults;
        }
        PlainSelect select;
        if (selectBody instanceof SetOperationList) {
            List<SelectBody> selects = ((SetOperationList) selectBody).getSelects();
            select = (PlainSelect) selects.get(selects.size() - 1);
        } else {
            select = (PlainSelect) selectBody;
        }

        List<OrderByElement> orderByElements = select.getOrderByElements();
        if (orderByElements == null) {
            return orderTableResults;
        }
        orderByElements.forEach(x -> {
            boolean selected = false;
//            for (TreeItem<TableRow> ddd : orderFieldsTree.getRoot().getChildren()) {
//                if (ddd.getValue().getName().equals(x.getExpression().toString())) {
//                    makeSelect(
//                            orderFieldsTree, orderTableResults, ddd, x.isAsc() ? "Ascending" : "Descending"
//                    );
//                    selected = true;
//                    break;
//                }
//            }
//            if (!selected) {
            TableRow tableRow = new TableRow(x.getExpression().toString());
            tableRow.setComboBoxValue(x.isAsc() ? "Ascending" : "Descending");
            orderTableResults.getItems().add(tableRow);
//            }
        });
        return orderTableResults;
    }

    @FXML
    protected void orderUp(ActionEvent event) {
        int index = orderTableResults.getSelectionModel().getSelectedIndex();
        orderTableResults.getItems().add(index - 1, orderTableResults.getItems().remove(index));
        orderTableResults.getSelectionModel().clearAndSelect(index - 1);
    }

    @FXML
    protected void orderDown(ActionEvent event) {
        int index = orderTableResults.getSelectionModel().getSelectedIndex();
        orderTableResults.getItems().add(index + 1, orderTableResults.getItems().remove(index));
        orderTableResults.getSelectionModel().clearAndSelect(index + 1);
    }

    @FXML
    protected void deselectOrder(ActionEvent event) {
        makeDeselect(orderTableResults, orderFieldsTree);
    }

    @FXML
    protected void selectOrder(ActionEvent event) {
        makeSelect(orderFieldsTree, orderTableResults, ORDER_DEFAULT_VALUE);
    }

}
