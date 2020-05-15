package com.querybuilder.querypart;

import com.querybuilder.controllers.MainController;
import com.querybuilder.domain.LinkElement;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SubSelect;

import java.util.List;

public class Links {

    public static void load(MainController controller, PlainSelect pSelect) {
        controller.getLinkTable().getItems().clear();
        List<Join> joins = pSelect.getJoins();
        if (joins == null) {
            return;
        }

        Table fromItem = (Table) pSelect.getFromItem();

        for (Join join : joins) {
            FromItem rightItem = join.getRightItem();
            if (rightItem instanceof Table) {
                addLinkRow(controller, fromItem, join);
            } else if (rightItem instanceof SubSelect) {
//                SubSelect sSelect = (SubSelect) rightItem;
//                rightItemName = sSelect.getAlias().getName();
//                TableRow tableRow = new TableRow(rightItemName);
//                tableRow.setNested(true);
//                tableRow.setRoot(true);
//                String queryText = sSelect.toString().replace(sSelect.getAlias().toString(), "");
//                queryText = queryText.substring(1, queryText.length() - 1);
//                tableRow.setQuery(queryText);
//                TreeItem<TableRow> tableRowTreeItem = new TreeItem<>(tableRow);
//                tablesView.getRoot().getChildren().add(tableRowTreeItem);
//
//                PlainSelect plainSelect = (PlainSelect) sSelect.getSelectBody();
//                plainSelect.getSelectItems().forEach((sItem) -> {
//                    TableRow nestedItem = new TableRow(sItem.toString());
//                    TreeItem<TableRow> nestedRow = new TreeItem<>(nestedItem);
//                    tableRowTreeItem.getChildren().add(nestedRow);
//                });

            }
        }
    }

    private static void addLinkRow(MainController controller, Table table, Join join) {
        if (join.getOnExpression() == null) {
            return;
        }

        Expression onExpression = join.getOnExpression();
        if (onExpression instanceof AndExpression) {
            AndExpression expression = (AndExpression) onExpression;
            while (true) {
                Expression rightExpression = expression.getRightExpression();
                LinkElement linkElement = new LinkElement(
                        controller, table.getName(), join.getRightItem().toString(),
                        isLeft(join), isRight(join), isCustom(rightExpression)
                );

                linkElement.setCondition(getSimpleCondition(rightExpression));
                controller.getLinkTable().getItems().add(linkElement);
                if (!(expression.getLeftExpression() instanceof AndExpression)) {
                    Expression lExpression = expression.getLeftExpression();
                    LinkElement linkElement2 = new LinkElement(
                            controller, table.getName(), join.getRightItem().toString(),
                            isLeft(join), isRight(join), isCustom(lExpression)
                    );
                    linkElement2.setCondition(getSimpleCondition(lExpression));
                    controller.getLinkTable().getItems().add(linkElement2);
                    break;
                }
                expression = (AndExpression) expression.getLeftExpression();
            }
        } else {
            Expression expression = join.getOnExpression();
            LinkElement linkElement = new LinkElement(
                    controller, table.getName(), join.getRightItem().toString(),
                    isLeft(join), isRight(join), isCustom(expression)
            );
            linkElement.setCondition(getSimpleCondition(expression));
            controller.getLinkTable().getItems().add(linkElement);
        }
    }

    private static String getSimpleCondition(Expression expression) {
        if (expression instanceof ComparisonOperator) {
            ComparisonOperator expr = (ComparisonOperator) expression;
            Column leftColumn = (Column) expr.getLeftExpression();
            Column rightColumn = (Column) expr.getRightExpression();
            return leftColumn.getColumnName() + expr.getStringExpression() + rightColumn.getColumnName();
        }
        System.out.println(expression);
        return expression.toString();
    }

    private static boolean isLeft(Join join) {
        if (join.isInner()) {
            return false;
        } else {
            return join.isFull() || join.isLeft();
        }
    }

    private static boolean isRight(Join join) {
        if (join.isInner()) {
            return false;
        } else {
            return join.isFull() || join.isRight();
        }
    }

    private static boolean isCustom(Expression expression) {
        return !(expression instanceof ComparisonOperator);
    }
}
