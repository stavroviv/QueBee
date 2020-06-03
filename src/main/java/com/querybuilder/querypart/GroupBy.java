package com.querybuilder.querypart;

import com.querybuilder.domain.TableRow;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.GroupByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;

import java.util.ArrayList;
import java.util.List;

import static com.querybuilder.utils.Constants.ALL_FIELDS;
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
        setCellFactory(groupFieldsTreeColumn);
    }

    @Override
    public void load(PlainSelect pSelect) {
        GroupByElement groupBy = pSelect.getGroupBy();
        if (groupBy == null) {
            return;
        }
        groupBy.getGroupByExpressions().forEach(x -> {
            for (TreeItem<TableRow> ddd : groupFieldsTree.getRoot().getChildren()) {
                if (ddd.getValue().getName().equals(x.toString())) {
                    makeSelect(groupFieldsTree, groupTableResults, ddd, null);
                    return;
                }
            }
            TableRow tableRow = new TableRow(x.toString());
            groupTableResults.getItems().add(0, tableRow);
        });
    }

    @Override
    public void save(PlainSelect pSelect) {
        if (groupTableResults.getItems().isEmpty()) {
            pSelect.setGroupByElement(null);
            return;
        }

        List<Expression> expressions = new ArrayList<>();
        for (TableRow item : groupTableResults.getItems()) {
            Column groupByItem = new Column(item.getName());
            expressions.add(groupByItem);
        }
        for (TreeItem<TableRow> child : groupFieldsTree.getRoot().getChildren()) {
            if (child.getValue().getName().equals(ALL_FIELDS)) {
                break;
            }
            Column groupByItem = new Column(child.getValue().getName());
            expressions.add(groupByItem);
        }

        GroupByElement groupByElement = new GroupByElement();
        groupByElement.setGroupByExpressions(expressions);
        pSelect.setGroupByElement(groupByElement);
    }

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
    private void deselectAggregate(ActionEvent event) {
        makeDeselect(groupTableAggregates, groupFieldsTree);
    }
}
