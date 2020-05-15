package com.querybuilder.querypart;

import com.querybuilder.controllers.MainController;
import com.querybuilder.domain.TableRow;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;

public class SelectedFields {

    public static void load(MainController controller, PlainSelect pSelect) {
        int id = 0;
        for (Object select : pSelect.getSelectItems()) {
            if (select instanceof SelectExpressionItem) {

                controller.getFieldTable().getItems().add(newTableRow(select.toString(), id));

                // GROUPING
                SelectExpressionItem select1 = (SelectExpressionItem) select;
                Expression expression1 = select1.getExpression();
                TableRow tableRow;
                if (expression1 instanceof Function) {
                    Function expression = (Function) select1.getExpression();
                    if (expression.getParameters().getExpressions().size() == 1) {
                        String columnName = expression.getParameters().getExpressions().get(0).toString();
                        tableRow = new TableRow(columnName);
                        tableRow.setComboBoxValue(expression.getName());
                        controller.getGroupTableAggregates().getItems().add(tableRow);
                    }
                }

            } else {
                controller.getFieldTable().getItems().add(newTableRow(select.toString(), id));
//                    aliasTable.getItems().add(newAliasItem(select));
            }
            id++;
        }
    }

    private static TableRow newTableRow(String name, int id) {
        TableRow tableRow1 = new TableRow(name);
        tableRow1.setId(id);
        return tableRow1;
    }
}
