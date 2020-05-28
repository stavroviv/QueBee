package com.querybuilder.querypart;

import com.querybuilder.controllers.MainController;
import com.querybuilder.domain.TableRow;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;

import java.util.ArrayList;
import java.util.List;

public class SelectedFields {

    public static void load(MainController controller, PlainSelect pSelect) {
        if (pSelect.getSelectItems() == null) {
            return;
        }

        for (Object select : pSelect.getSelectItems()) {
            if (select instanceof SelectExpressionItem) {

                controller.getFieldTable().getItems().add(
                        new TableRow(select.toString())
                );

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
                controller.getFieldTable().getItems().add(
                        new TableRow(select.toString())
                );
            }
        }
    }

    public static void save(MainController controller, PlainSelect selectBody) {
        List<SelectItem> items = new ArrayList<>();
        controller.getFieldTable().getItems().forEach(x -> {
            SelectExpressionItem sItem = new SelectExpressionItem();
            sItem.setExpression(new Column(x.getName()));
            items.add(sItem);
        });
        selectBody.setSelectItems(items);
    }

}
