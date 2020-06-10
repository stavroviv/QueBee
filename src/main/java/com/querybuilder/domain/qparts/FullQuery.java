package com.querybuilder.domain.qparts;

import com.querybuilder.domain.AliasRow;
import com.querybuilder.domain.ConditionElement;
import com.querybuilder.domain.LinkElement;
import com.querybuilder.domain.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import lombok.Data;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;

import java.util.*;

import static com.querybuilder.utils.Constants.*;

@Data
public class FullQuery {
    private Map<String, OneCte> cteMap = new LinkedHashMap<>();

    public FullQuery() {
        cteMap.put(CTE_0, new OneCte());
    }

    public Select getQuery() {
        Select select = new Select();

        List<WithItem> withItems = new ArrayList<>();
        Iterator<String> iterator = cteMap.keySet().iterator();
        String cte;
        int index = 0;

        while (iterator.hasNext()) {
            cte = iterator.next();
            if (index == cteMap.size() - 1) {
                select.setSelectBody(getSelectBody(cte));
                break;
            }
            WithItem cteBody = new WithItem();
            cteBody.setName(cte);
            cteBody.setSelectBody(getSelectBody(cte));
            withItems.add(cteBody);

            index++;
        }
        if (!withItems.isEmpty()) {
            select.setWithItemsList(withItems);
        }

        return select.toString().equals(EMPTY_SELECT) ? null : select;
    }

    private SelectBody getSelectBody(String cte) {
        SetOperationList selectBody = new SetOperationList();
        OneCte oneCte = cteMap.get(cte);

        Iterator<String> iterator = oneCte.getUnionMap().keySet().iterator();
        String union;

        List<SetOperation> ops = new ArrayList<>();
        List<Boolean> brackets = new ArrayList<>();
        List<SelectBody> selectBodies = new ArrayList<>();
        boolean first = true;
        while (iterator.hasNext()) {
            union = iterator.next();
            if (oneCte.getUnionMap().size() == 1) {
                return getPlainSelect(oneCte, union, first, true);
            }
            brackets.add(false);
            UnionOp unionOp = new UnionOp();
            unionOp.setAll(true);
            ops.add(unionOp);
            selectBodies.add(getPlainSelect(oneCte, union, first, !iterator.hasNext()));
            first = false;
        }
        ops.remove(ops.size() - 1);
        selectBody.setBracketsOpsAndSelects(brackets, selectBodies, ops);

        return selectBody;
    }

    private PlainSelect getPlainSelect(OneCte cte, String union, boolean first, boolean last) {
        PlainSelect select = new PlainSelect();

        saveAliases(select, cte, union, first);
        saveFromTables(select, cte, union);
        saveLinks(select, cte, union);
        saveGroupBy(select, cte, union);
        saveConditions(select, cte, union);
        if (last) {
            saveOrderBy(select, cte);
        }

        return select;
    }

    public static void saveLinks(PlainSelect selectBody, OneCte cte, String union) {
        Union union1 = cte.getUnionMap().get(union);
        TableView<LinkElement> linkTable = union1.getLinkTable();
        if (linkTable.getItems().isEmpty()) {
            return;
        }

        // 1. если JOIN есть - то надо указать связи всех таблиц
        // 2. RIGHT JOIN изменить на LEFT и упорядочить все строки кроме первой
        String tableFrom = linkTable.getItems().get(0).getTable1();
        selectBody.setFromItem(new Table(tableFrom));
        List<Join> joins = new ArrayList<>();
        for (LinkElement item : linkTable.getItems()) {
            Join join = new Join();
            join.setRightItem(new Table(item.getTable2()));
            setJoinType(item, join);
            setCondition(item, join);
            joins.add(join);
        }

        selectBody.setJoins(joins);
    }

    private static void setCondition(LinkElement item, Join join) {
        String strExpression = item.getCondition();
        if (!item.isCustom()) {
            strExpression = item.getTable1() + "." + item.getField1()
                    + item.getExpression()
                    + item.getTable2() + "." + item.getField2();
        }
        try {
            join.setOnExpression(getExpression(strExpression));
        } catch (JSQLParserException e) {
            e.printStackTrace();
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

    private static void saveConditions(PlainSelect selectBody, OneCte cte, String unionName) {
        Union union = cte.getUnionMap().get(unionName);
        TableView<ConditionElement> conditionTableResults = union.getConditionTableResults();
        if (conditionTableResults.getItems().size() == 0) {
            selectBody.setWhere(null);
            return;
        }
        StringBuilder where = new StringBuilder();
        for (ConditionElement item : conditionTableResults.getItems()) {
            String whereExpr = item.getCondition();
            if (whereExpr.isEmpty()) {
                whereExpr = item.getLeftExpression() + item.getExpression() + item.getRightExpression();
            }
            where.append(whereExpr).append(" AND ");
        }
        Statement stmt = null;
        try {
            stmt = CCJSqlParserUtil.parse(
                    "SELECT * FROM TABLES WHERE " + where.substring(0, where.length() - 4)
            );
        } catch (JSQLParserException e) {
            e.printStackTrace();
        }
        Select select = (Select) stmt;
        Expression whereExpression = ((PlainSelect) select.getSelectBody()).getWhere();
        selectBody.setWhere(whereExpression);
    }

    private static void saveGroupBy(PlainSelect select, OneCte cte, String union) {
        Union union1 = cte.getUnionMap().get(union);
        TableView<TableRow> groupTableResults = union1.getGroupTableResults();
        if (groupTableResults.getItems().isEmpty()) {
            return;
        }
        //  if (groupTableResults.getItems().isEmpty() && groupTableAggregates.getItems().isEmpty()) {
        if (groupTableResults.getItems().isEmpty()) {
            select.setGroupByElement(null);
            return;
        }
        List<Expression> expressions = new ArrayList<>();
        for (TableRow item : groupTableResults.getItems()) {
            Column groupByItem = new Column(item.getName());
            expressions.add(groupByItem);
        }
        for (TreeItem<TableRow> child : union1.getGroupFieldsTree().getRoot().getChildren()) {
            if (child.getValue().getName().equals(ALL_FIELDS)) {
                break;
            }
            Column groupByItem = new Column(child.getValue().getName());
            expressions.add(groupByItem);
        }

        if (expressions.isEmpty()) {
            select.setGroupByElement(null);
            return;
        }
        GroupByElement groupByElement = new GroupByElement();
        groupByElement.setGroupByExpressions(expressions);
        select.setGroupByElement(groupByElement);
    }

    private static void setAggregate(SelectExpressionItem sItem, Long id, OneCte cte, String union) {
        if (id == null) {
            return;
        }
        Union union1 = cte.getUnionMap().get(union);
        TableView<TableRow> groupTableAggregates = union1.getGroupTableAggregates();
        if (groupTableAggregates.getItems().isEmpty()) {
            return;
        }

        for (TableRow x : groupTableAggregates.getItems()) {
            if (id != x.getId()) {
                continue;
            }
            Function expression = new Function();
            expression.setName(x.getComboBoxValue());
            ExpressionList list = new ExpressionList();
            Column col = new Column(x.getName());
            list.setExpressions(Collections.singletonList(col));
            expression.setParameters(list);
            sItem.setExpression(expression);
        }
    }

    private static void saveAliases(PlainSelect select, OneCte cte, String union, boolean first) {
        List<SelectItem> sItems = new ArrayList<>();

        for (AliasRow item : cte.getAliasTable().getItems()) {
            String name = item.getValues().get(union);
            if (name.equals(EMPTY_UNION_VALUE)) {
                name = "NULL";
            }

            Expression expression = null;
            try {
                expression = new CCJSqlParser(name).Expression();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (expression == null) {
                throw new RuntimeException("Empty expression for " + name);
            }

            SelectExpressionItem sItem = new SelectExpressionItem();
            sItem.setExpression(expression);

            Expression expression1 = sItem.getExpression();
            boolean equals = true;
            if (expression1 instanceof Column) {
                Column column = (Column) expression1;
                equals = !column.getColumnName().equals(item.getAlias());
            }

            if (first && equals) {
                sItem.setAlias(new Alias(item.getAlias()));
            }
            setAggregate(sItem, item.getIds().get(union), cte, union);
            sItems.add(sItem);
        }
        select.setSelectItems(sItems);
    }

    private static void saveFromTables(PlainSelect selectBody, OneCte oneCte, String union) {
        Union union1 = oneCte.getUnionMap().get(union);
        if (!union1.getLinkTable().getItems().isEmpty()) {
            return;
        }
        List<Join> joins = new ArrayList<>();
        union1.getTablesView().getRoot().getChildren().forEach(x -> {
            String tableName = x.getValue().getName();
            if (selectBody.getFromItem() == null) {
                selectBody.setFromItem(new Table(tableName));
            } else {
                Join join = new Join();
                join.setRightItem(new Table(tableName));
                join.setSimple(true);
                joins.add(join);
            }
        });
        selectBody.setJoins(joins);
    }

    public void saveOrderBy(PlainSelect select, OneCte cte) {
        List<OrderByElement> orderElements = new ArrayList<>();

        cte.getOrderTableResults().getItems().forEach(x -> {
            OrderByElement orderByElement = new OrderByElement();
            Column column = new Column(x.getName());
            orderByElement.setExpression(column);
            orderByElement.setAsc(x.getComboBoxValue().equals("Ascending"));
            orderElements.add(orderByElement);
        });

        select.setOrderByElements(orderElements);
    }

}
