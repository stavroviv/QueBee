package com.querybuilder.domain.qparts;

import com.querybuilder.controllers.MainController;
import com.querybuilder.domain.AliasRow;
import com.querybuilder.domain.TableRow;
import com.querybuilder.querypart.OrderBy;
import com.querybuilder.querypart.UnionAliases;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeTableView;
import lombok.Data;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.querybuilder.utils.Constants.UNION_0;
import static com.querybuilder.utils.Utils.loadTableToTable;

@Data
public class OneCte implements Orderable {
    private String cteName;
    private Integer order;
    private Map<String, Union> unionMap = new LinkedHashMap<>();
    private Map<String, TableColumn<AliasRow, String>> unionColumns = new HashMap<>();

    private TableView<AliasRow> aliasTable = new TableView<>();
    private TableView<TableRow> unionTable = new TableView<>();

    private TreeTableView<TableRow> orderFieldsTree = new TreeTableView<>();
    private TableView<TableRow> orderTableResults = new TableView<>();
    private int curMaxUnion;

    public OneCte(String cteName, Integer order) {
        this.cteName = cteName;
        this.order = order;
        unionMap.put(UNION_0, new Union(0));
    }

    public OneCte(MainController controller, String cteName, Integer order) {
        this(cteName, order);
        aliasTable.getColumns().add(UnionAliases.aliasColumn());
        UnionAliases.addUnionColumn(aliasTable, unionTable, UNION_0, this, controller, false);
    }

    public OneCte(MainController controller, OneCte currentCte) {
        loadTableToTable(currentCte.getAliasTable(), aliasTable);
        loadTableToTable(currentCte.getUnionTable(), unionTable);
        loadTableToTable(currentCte.getOrderTableResults(), orderTableResults);
        int index = 0;
        for (Map.Entry<String, Union> stringUnionEntry : currentCte.getUnionMap().entrySet()) {
            Union union = new Union(index);
            loadTableToTable(stringUnionEntry.getValue().getLinkTable(), union.getLinkTable());
            loadTableToTable(stringUnionEntry.getValue().getFieldTable(), union.getFieldTable());

            String unionId = stringUnionEntry.getKey();
            unionMap.put(unionId, union);

            aliasTable.getColumns().add(UnionAliases.aliasColumn());
            UnionAliases.addUnionColumn(aliasTable, unionTable, unionId, this, controller, false);
            index++;
        }
    }

    public void saveAliasTable(UnionAliases controller) {
        aliasTable.getItems().clear();
        TableView<AliasRow> newAliasTable = controller.getAliasTable();
        aliasTable.getColumns().clear();
        for (TableColumn<AliasRow, ?> column : newAliasTable.getColumns()) {
            aliasTable.getColumns().add(column);
        }
        aliasTable.getItems().addAll(controller.getAliasTable().getItems());

        loadTableToTable(controller.getUnionTable(), unionTable);
    }

    public void showAliasTable(UnionAliases controller) {
        controller.getAliasTable().getItems().clear();
        controller.getAliasTable().getColumns().clear();
        for (TableColumn<AliasRow, ?> column : aliasTable.getColumns()) {
            controller.getAliasTable().getColumns().add(column);
        }
        controller.getAliasTable().getItems().addAll(aliasTable.getItems());

        loadTableToTable(unionTable, controller.getUnionTable());
    }

    public void saveOrderBy(OrderBy controller) {
        loadTableToTable(controller.getOrderTableResults(), orderTableResults);
    }

    public void showOrderBy(OrderBy controller) {
        loadTableToTable(orderTableResults, controller.getOrderTableResults());
    }

}
