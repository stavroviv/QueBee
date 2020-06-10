package com.querybuilder.querypart;

import com.querybuilder.domain.TableRow;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.GroupByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;

import static com.querybuilder.utils.Constants.GROUP_DEFAULT_VALUE;
import static com.querybuilder.utils.Utils.*;

@Data
@EqualsAndHashCode(callSuper = false)
public class GroupBy extends AbstractQueryPart {
    @FXML
    private TreeTableView<TableRow> groupFieldsTree;
    @FXML
    private TreeTableColumn<TableRow, TableRow> groupFieldsTreeColumn;
    @FXML
    private TableView<TableRow> groupTableResults;
    @FXML
    private TableColumn<TableRow, String> groupTableResultsFieldColumn;
    @FXML
    private TableView<TableRow> groupTableAggregates;
    @FXML
    private TableColumn<TableRow, String> groupTableAggregatesFieldColumn;
    @FXML
    private TableColumn<TableRow, String> groupTableAggregatesFunctionColumn;

    @Override
    public void initialize() {
        setStringColumnFactory(groupTableResultsFieldColumn);
        setStringColumnFactory(groupTableAggregatesFieldColumn);

        setCellSelectionEnabled(groupTableAggregates);
        setComboBoxColumnFactory(groupTableAggregatesFunctionColumn,
                GROUP_DEFAULT_VALUE, "AVG", "COUNT", "MIN", "MAX");

        setTreeSelectHandler(groupFieldsTree, groupTableResults);
        setResultsTableSelectHandler(groupTableResults, groupFieldsTree);
        setResultsTableSelectHandler(groupTableAggregates, groupFieldsTree);
        setCellFactory(groupFieldsTreeColumn);
    }

    public TableView<TableRow> loadGroupBy(PlainSelect pSelect) {
        TableView<TableRow> groupTableResults = new TableView<>();
        GroupByElement groupBy = pSelect.getGroupBy();
        if (groupBy == null) {
            return groupTableResults;
        }
        groupBy.getGroupByExpressions().forEach(x -> {
            if (x instanceof Column) {
                setTableName(pSelect, (Column) x);
            }
            TableRow tableRow = new TableRow(x.toString());
            groupTableResults.getItems().add(0, tableRow);
        });
        return groupTableResults;
    }

    private void setTableName(PlainSelect pSelect, Column expression) {
        if (expression.getTable() == null && pSelect.getJoins() == null) {
            expression.setTable((Table) pSelect.getFromItem());
        }
    }

//    private void deselectAggregates() {
//        for (TableRow item : groupTableAggregates.getItems()) {
//            for (TreeItem<TableRow> child : groupFieldsTree.getRoot().getChildren()) {
//                if (child.getValue().getId() == item.getId()) {
//                    groupFieldsTree.getRoot().getChildren().remove(child);
//                    break;
//                }
//            }
//        }
//    }
//
//    public boolean containAggregate(TableRow field) {
//        for (TableRow x : groupTableAggregates.getItems()) {
//            if (x.getId() == field.getId()) {
//                return true;
//            }
//        }
//        return false;
//    }

    @FXML
    private void selectGroup(ActionEvent event) {
        makeSelect(groupFieldsTree, groupTableResults);
    }

    @FXML
    private void selectGroupAll(ActionEvent event) {
        makeSelectAll(groupFieldsTree, groupTableResults);
    }

    @FXML
    private void deselectGroup(ActionEvent event) {
        makeDeselect(groupTableResults, groupFieldsTree);
    }

    @FXML
    private void deselectGroupAll(ActionEvent event) {
        makeDeselectAll(groupTableResults, groupFieldsTree);
    }

    @FXML
    private void selectAggregate(ActionEvent event) {
        makeSelect(groupFieldsTree, groupTableAggregates, GROUP_DEFAULT_VALUE);
    }

    @FXML
    private void selectAggregateAll(ActionEvent event) {
        makeSelectAll(groupFieldsTree, groupTableAggregates, GROUP_DEFAULT_VALUE);
    }

    @FXML
    private void deselectAggregate(ActionEvent event) {
        makeDeselect(groupTableAggregates, groupFieldsTree);
    }

    @FXML
    private void deselectAggregateAll(ActionEvent event) {
        makeDeselectAll(groupTableAggregates, groupFieldsTree);
    }
}
