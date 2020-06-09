package com.querybuilder.querypart;

import com.querybuilder.domain.TableRow;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.statement.select.GroupByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;

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

    public void load(PlainSelect pSelect) {
        loadGroupBy(pSelect);
        deselectAggregates();
    }

    public TableView<TableRow> loadGroupBy(PlainSelect pSelect) {
        TableView<TableRow> groupTableResults = new TableView<>();
        GroupByElement groupBy = pSelect.getGroupBy();
        if (groupBy == null) {
            return groupTableResults;
        }
        groupBy.getGroupByExpressions().forEach(x -> {
//            for (TreeItem<TableRow> ddd : groupFieldsTree.getRoot().getChildren()) {
//                if (ddd.getValue().getName().equals(x.toString())) {
//                    makeSelect(groupFieldsTree, groupTableResults, ddd, null);
//                    return;
//                }
//            }
            TableRow tableRow = new TableRow(x.toString());
            groupTableResults.getItems().add(0, tableRow);
        });
        return groupTableResults;
    }

    private void deselectAggregates() {
        for (TableRow item : groupTableAggregates.getItems()) {
            for (TreeItem<TableRow> child : groupFieldsTree.getRoot().getChildren()) {
                if (child.getValue().getId() == item.getId()) {
                    groupFieldsTree.getRoot().getChildren().remove(child);
                    break;
                }
            }
        }
    }


    public TableView<TableRow> loadAggregates(PlainSelect pSelect) {
//        TableRow tableRow = TableRow.tableRowFromValue(newField);
//        tableRow.setComboBoxValue(function.getName());
        TableView<TableRow> groupTableAggregates = new TableView<>();
        for (SelectItem selectField : pSelect.getSelectItems()) {
            if (!(selectField instanceof SelectExpressionItem)) {
                continue;
            }

            // GROUPING
            SelectExpressionItem item = (SelectExpressionItem) selectField;
            Expression expression = item.getExpression();

            if (expression instanceof Function
                    && ((Function) item.getExpression()).getParameters().getExpressions().size() == 1) {
                Function function = (Function) item.getExpression();
                String columnName = function.getParameters().getExpressions().get(0).toString();
                TableRow newField = new TableRow(columnName);

                // fieldTable.getItems().add(newField);
                TableRow tableRow = TableRow.tableRowFromValue(newField);
                tableRow.setComboBoxValue(function.getName());
                groupTableAggregates.getItems().add(tableRow);

            }
        }

        return groupTableAggregates;
    }

    public void save(PlainSelect pSelect) {
//        saveGroupBy(pSelect);
        //saveAggregates(pSelect);
    }

//    private void saveGroupBy(PlainSelect pSelect) {
//        if (groupTableResults.getItems().isEmpty() && groupTableAggregates.getItems().isEmpty()) {
//            pSelect.setGroupByElement(null);
//            return;
//        }
//        List<Expression> expressions = new ArrayList<>();
//        for (TableRow item : groupTableResults.getItems()) {
//            Column groupByItem = new Column(item.getName());
//            expressions.add(groupByItem);
//        }
//        for (TreeItem<TableRow> child : groupFieldsTree.getRoot().getChildren()) {
//            if (child.getValue().getName().equals(ALL_FIELDS)) {
//                break;
//            }
//            Column groupByItem = new Column(child.getValue().getName());
//            expressions.add(groupByItem);
//        }
//
//        if (expressions.isEmpty()) {
//            pSelect.setGroupByElement(null);
//            return;
//        }
//        GroupByElement groupByElement = new GroupByElement();
//        groupByElement.setGroupByExpressions(expressions);
//        pSelect.setGroupByElement(groupByElement);
//    }

//    private void saveAggregates(PlainSelect pSelect) {
//        if (groupTableAggregates.getItems().isEmpty()) {
//            return;
//        }
//
//        // add aggregates
//        List<SelectItem> selectItems = pSelect.getSelectItems();
//        for (TableRow x : groupTableAggregates.getItems()) {
//            SelectExpressionItem sItem = new SelectExpressionItem();
//            Function expression = new Function();
//            expression.setName(x.getComboBoxValue());
//            ExpressionList list = new ExpressionList();
//            Column col = new Column(x.getName());
//            list.setExpressions(Collections.singletonList(col));
//            expression.setParameters(list);
//            sItem.setExpression(expression);
//            selectItems.add(sItem);
//        }
//        pSelect.setSelectItems(selectItems);
//    }

    public boolean containAggregate(TableRow field) {
        for (TableRow x : groupTableAggregates.getItems()) {
            if (x.getId() == field.getId()) {
                return true;
            }
        }
        return false;
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
