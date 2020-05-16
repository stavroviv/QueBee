package com.querybuilder.querypart;

import com.querybuilder.controllers.MainController;
import com.querybuilder.domain.JoinConditionCell;
import com.querybuilder.domain.LinkElement;
import com.querybuilder.domain.LinkTableCell;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.cell.CheckBoxTableCell;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;

import java.util.ArrayList;
import java.util.List;

public class Links implements QueryPart {

    public static void init(MainController controller) {
        controller.getLinkTable().setEditable(true);

        controller.getLinkTableAllTable1().setCellFactory(tc -> new CheckBoxTableCell<>());
        controller.getLinkTableAllTable2().setCellFactory(tc -> new CheckBoxTableCell<>());

        controller.getLinkTableCustom().setCellFactory(column -> new CheckBoxTableCell<>());
        controller.getLinkTableCustom().setCellValueFactory(cellData -> {
            LinkElement cellValue = cellData.getValue();
            BooleanProperty property = cellValue.customProperty();
            property.addListener((observable, oldValue, newValue) -> {
                cellValue.setCustom(newValue);
                controller.refreshLinkTable();
            });
            return property;
        });

        controller.getLinkTableColumnTable1().setCellValueFactory(column -> new ReadOnlyObjectWrapper<>(column.getValue()));
        controller.getLinkTableColumnTable1().setCellFactory(param -> new LinkTableCell(controller, "table1"));

        controller.getLinkTableColumnTable2().setCellValueFactory(column -> new ReadOnlyObjectWrapper<>(column.getValue()));
        controller.getLinkTableColumnTable2().setCellFactory(param -> new LinkTableCell(controller, "table2"));

        controller.getLinkTableJoinCondition().setCellValueFactory(features -> new ReadOnlyObjectWrapper<>(features.getValue()));
        controller.getLinkTableJoinCondition().setCellFactory(column -> new JoinConditionCell());
    }

    @Override
    public void load(MainController controller, PlainSelect pSelect) {
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

    @Override
    public void save(MainController controller, PlainSelect selectBody) throws Exception {
        if (controller.getLinkTable().getItems().isEmpty()) {
            return;
        }
        // 1. если JOIN есть - то надо указать связи всех таблиц
        // 2. RIGHT JOIN изменить на LEFT и упорядочить все строки кроме первой

        String tableFrom = controller.getLinkTable().getItems().get(0).getTable1();
        selectBody.setFromItem(new Table(tableFrom));
        List<Join> joins = new ArrayList<>();
        for (LinkElement item : controller.getLinkTable().getItems()) {
            Join join = new Join();
            join.setRightItem(new Table(item.getTable2()));
            setJoinType(item, join);
            setCondition(item, join);
            joins.add(join);
        }

        selectBody.setJoins(joins);
    }

    private static void setCondition(LinkElement item, Join join) throws Exception {
        String strExpression = item.getCondition();
        if (!item.isCustom()) {
            strExpression = item.getTable1() + "." + item.getField1()
                    + item.getExpression()
                    + item.getTable2() + "." + item.getField2();
        }
        try {
            join.setOnExpression(getExpression(strExpression));
        } catch (JSQLParserException e) {
            throw new Exception(e);
        }
    }

    private static void setJoinType(LinkElement item, Join join) {
        if (item.isAllTable1() && item.isAllTable2()) {
            join.setFull(true);
        } else if (item.isAllTable1()) {
            join.setLeft(true);
        } else if (item.isAllTable2()) {
            join.setRight(true);
        } else {
            join.setInner(true);
        }
    }

    private static Expression getExpression(String where) throws JSQLParserException {
        Statement stmt = CCJSqlParserUtil.parse(
                "SELECT * FROM TABLES WHERE " + where
        );
        Select select = (Select) stmt;
        return ((PlainSelect) select.getSelectBody()).getWhere();
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
