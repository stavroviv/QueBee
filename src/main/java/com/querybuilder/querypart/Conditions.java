package com.querybuilder.querypart;

import com.querybuilder.controllers.MainController;
import com.querybuilder.domain.ConditionCell;
import com.querybuilder.domain.ConditionElement;
import com.querybuilder.domain.TableRow;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.TreeItem;
import javafx.scene.control.cell.CheckBoxTableCell;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;

import static com.querybuilder.utils.Constants.DATABASE_ROOT;
import static com.querybuilder.utils.Utils.doubleClick;

public class Conditions {

    public static void init(MainController controller) {
        controller.getConditionTableResults().setEditable(true);
        controller.getConditionTableResults().getSelectionModel().cellSelectionEnabledProperty().set(true);

        controller.getConditionTableResultsCustom().setCellFactory(column -> new CheckBoxTableCell<>());
        controller.getConditionTableResultsCustom().setCellValueFactory(cellData -> {
            ConditionElement cellValue = cellData.getValue();
            BooleanProperty property = cellValue.customProperty();
            property.addListener((observable, oldValue, newValue) -> {
                cellValue.setCustom(newValue);
                controller.getConditionTableResults().refresh();
            });
            return property;
        });

        controller.getConditionTableResultsCondition().setCellValueFactory(
                column -> new ReadOnlyObjectWrapper<>(column.getValue())
        );
        controller.getConditionTableResultsCondition().setCellFactory(
                column -> new ConditionCell(controller.getConditionTableResults(), controller.getTablesView())
        );

        controller.getConditionsTreeTable().setOnMousePressed(e -> {
            if (!doubleClick(e)) {
                return;
            }
            TreeItem<TableRow> selectedItem = controller.getConditionsTreeTable().getSelectionModel().getSelectedItem();
            if (selectedItem == null || selectedItem.getChildren().size() > 0) {
                return;
            }
            String name = selectedItem.getValue().getName();
            TreeItem<TableRow> parent = selectedItem.getParent();
            if (parent != null) {
                String parentName = parent.getValue().getName();
                if (!parentName.equals(DATABASE_ROOT)) {
                    name = parentName + "." + name;
                }
            }
            ConditionElement tableRow = new ConditionElement(name);
            controller.getConditionTableResults().getItems().add(tableRow);
            controller.getConditionsTreeTable().getRoot().getChildren().remove(selectedItem);
        });
    }

    public static void load(MainController controller, PlainSelect pSelect) {
        Expression where = pSelect.getWhere();
        if (where == null) {
            return;
        }
        if (where instanceof AndExpression) {
            parseAndExpression(controller, (AndExpression) where);
        } else {
            ConditionElement conditionElement = new ConditionElement(where.toString());
            conditionElement.setCustom(true);
            controller.getConditionTableResults().getItems().add(conditionElement);
        }
    }

    private static void parseAndExpression(MainController controller, AndExpression where) {
        ConditionElement conditionElement = new ConditionElement(where.getRightExpression().toString());
        controller.getConditionTableResults().getItems().add(0, conditionElement);

        Expression leftExpression = where.getLeftExpression();
        while (leftExpression instanceof AndExpression) {
            AndExpression left = (AndExpression) leftExpression;
            ConditionElement condition = new ConditionElement(left.getRightExpression().toString());
            controller.getConditionTableResults().getItems().add(0, condition);
            leftExpression = left.getLeftExpression();
        }
        ConditionElement condition = new ConditionElement(leftExpression.toString());
        controller.getConditionTableResults().getItems().add(0, condition);
    }

    public static void save(MainController controller, PlainSelect selectBody) throws Exception {
        if (controller.getConditionTableResults().getItems().size() == 0) {
            selectBody.setWhere(null);
            return;
        }
        StringBuilder where = new StringBuilder();
        for (ConditionElement item : controller.getConditionTableResults().getItems()) {
            String whereExpr = item.getCondition();
            if (whereExpr.isEmpty()) {
                whereExpr = item.getLeftExpression() + item.getExpression() + item.getRightExpression();
            }
            where.append(whereExpr).append(" AND ");
        }
        Statement stmt = CCJSqlParserUtil.parse(
                "SELECT * FROM TABLES WHERE " + where.substring(0, where.length() - 4)
        );
        Select select = (Select) stmt;
        Expression whereExpression = ((PlainSelect) select.getSelectBody()).getWhere();
        selectBody.setWhere(whereExpression);
    }
}
