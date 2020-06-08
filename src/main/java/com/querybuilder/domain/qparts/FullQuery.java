package com.querybuilder.domain.qparts;

import lombok.Data;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.parser.ParseException;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;

import java.util.*;

import static com.querybuilder.utils.Constants.CTE_0;

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

        System.out.println(select.toString());
        return select;
    }

    private SelectBody getSelectBody(String cte) {
        SetOperationList selectBody = new SetOperationList();
        OneCte oneCte = cteMap.get(cte);

        Iterator<String> iterator = oneCte.getUnionMap().keySet().iterator();
        String union;

        List<SetOperation> ops = new ArrayList<>();
        List<Boolean> brackets = new ArrayList<>();
        List<SelectBody> selectBodies = new ArrayList<>();

        while (iterator.hasNext()) {
            union = iterator.next();
            if (oneCte.getUnionMap().size() == 1) {
                return getPlainSelect(oneCte, union);
            }
            brackets.add(false);
            UnionOp unionOp = new UnionOp();
            unionOp.setAll(true);
            ops.add(unionOp);
            selectBodies.add(getPlainSelect(oneCte, union));
        }
        ops.remove(ops.size() - 1);
        selectBody.setBracketsOpsAndSelects(brackets, selectBodies, ops);

        return selectBody;
    }

    private PlainSelect getPlainSelect(OneCte cte, String union) {
        PlainSelect select = new PlainSelect();

        saveAliases(select, cte, union);
        saveFromTables(select, cte, union);

        return select;
    }

    private static void saveAliases(PlainSelect select, OneCte cte, String union) {
        List<SelectItem> sItems = new ArrayList<>();
        cte.getAliasTable().getItems().forEach(item -> {
            String name = item.getValues().get(union);
            Expression expression = null;
            try {
                expression = new CCJSqlParser(name).Expression();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            if (expression == null) { // FIXME
                return;
            }
            SelectExpressionItem sItem = new SelectExpressionItem();
            sItem.setExpression(expression);
            sItem.setAlias(new Alias(item.getAlias()));
            sItems.add(sItem);
        });
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

}
