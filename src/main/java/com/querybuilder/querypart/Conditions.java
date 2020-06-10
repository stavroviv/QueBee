package com.querybuilder.querypart;

import com.querybuilder.domain.ConditionCell;
import com.querybuilder.domain.ConditionElement;
import com.querybuilder.domain.TableRow;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.statement.select.PlainSelect;

import static com.querybuilder.utils.Constants.DATABASE_TABLE_ROOT;
import static com.querybuilder.utils.Utils.doubleClick;
import static com.querybuilder.utils.Utils.setCellFactory;

@Data
@EqualsAndHashCode(callSuper = false)
public class Conditions extends AbstractQueryPart {

    @FXML
    private TreeTableView<TableRow> conditionsTreeTable;
    @FXML
    private TreeTableColumn<TableRow, TableRow> conditionsTreeTableColumn;
    @FXML
    private TableView<ConditionElement> conditionTableResults;
    @FXML
    private TableColumn<ConditionElement, Boolean> conditionTableResultsCustom;
    @FXML
    private TableColumn<ConditionElement, ConditionElement> conditionTableResultsCondition;

    @FXML
    public void addCondition() {
        conditionTableResults.getItems().add(new ConditionElement(""));
    }

    @FXML
    public void deleteCondition() {
        ConditionElement selectedItem = conditionTableResults.getSelectionModel().getSelectedItem();
        conditionTableResults.getItems().remove(selectedItem);
    }

    @FXML
    public void copyCondition() {
        ConditionElement selectedItem = conditionTableResults.getSelectionModel().getSelectedItem();
        ConditionElement conditionElement = new ConditionElement("");
        conditionElement.setName(selectedItem.getName());
        conditionTableResults.getItems().add(conditionElement);
    }

    @Override
    public void initialize() {
        conditionTableResults.setEditable(true);
        conditionTableResults.getSelectionModel().cellSelectionEnabledProperty().set(true);

        conditionTableResultsCustom.setCellFactory(column -> new CheckBoxTableCell<>());
        conditionTableResultsCustom.setCellValueFactory(cellData -> {
            ConditionElement cellValue = cellData.getValue();
            BooleanProperty property = cellValue.customProperty();
            property.addListener((observable, oldValue, newValue) -> {
                cellValue.setCustom(newValue);
                conditionTableResults.refresh();
            });
            return property;
        });

        conditionTableResultsCondition.setCellValueFactory(column -> new ReadOnlyObjectWrapper<>(column.getValue()));
        conditionTableResultsCondition.setCellFactory(column -> new ConditionCell(
                conditionTableResults, mainController.getTableFieldsController().getTablesView()
        ));

        setListeners();
        setCellFactory(conditionsTreeTableColumn);
    }

    private void setListeners() {
        conditionsTreeTable.setOnMousePressed(e -> {
            if (!doubleClick(e)) {
                return;
            }
            TreeItem<TableRow> selectedItem = conditionsTreeTable.getSelectionModel().getSelectedItem();
            if (selectedItem == null || selectedItem.getChildren().size() > 0) {
                return;
            }
            String name = selectedItem.getValue().getName();
            TreeItem<TableRow> parent = selectedItem.getParent();
            if (parent != null) {
                String parentName = parent.getValue().getName();
                if (!parentName.equals(DATABASE_TABLE_ROOT)) {
                    name = parentName + "." + name;
                }
            }
            ConditionElement tableRow = new ConditionElement(name);
            conditionTableResults.getItems().add(tableRow);
            conditionsTreeTable.getRoot().getChildren().remove(selectedItem);
        });
    }

    public TableView<ConditionElement> load(PlainSelect pSelect) {
        TableView<ConditionElement> conditionTableResults = new TableView<>();
        Expression where = pSelect.getWhere();
        if (where == null) {
            return conditionTableResults;
        }
        if (where instanceof AndExpression) {
            parseAndExpression(conditionTableResults, (AndExpression) where);
        } else {
            ConditionElement conditionElement = new ConditionElement(where.toString());
            conditionElement.setCustom(!(where instanceof ComparisonOperator));
            conditionTableResults.getItems().add(conditionElement);
        }
        return conditionTableResults;
    }

    private void parseAndExpression(TableView<ConditionElement> conditionTableResults, AndExpression where) {
        ConditionElement conditionElement = new ConditionElement(where.getRightExpression().toString());
        conditionTableResults.getItems().add(0, conditionElement);

        Expression leftExpression = where.getLeftExpression();
        while (leftExpression instanceof AndExpression) {
            AndExpression left = (AndExpression) leftExpression;
            ConditionElement condition = new ConditionElement(left.getRightExpression().toString());
            conditionTableResults.getItems().add(0, condition);
            leftExpression = left.getLeftExpression();
        }
        ConditionElement condition = new ConditionElement(leftExpression.toString());
        conditionTableResults.getItems().add(0, condition);
    }

}
