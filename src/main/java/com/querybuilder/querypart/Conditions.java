package com.querybuilder.querypart;

import com.querybuilder.controllers.MainController;
import com.querybuilder.domain.ConditionElement;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;

public class Conditions implements QueryPart {

    @Override
    public void load(MainController controller, PlainSelect pSelect) {
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

    @Override
    public void save(MainController controller, PlainSelect selectBody) throws Exception {
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
