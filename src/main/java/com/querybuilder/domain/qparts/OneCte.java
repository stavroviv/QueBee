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
public class OneCte {
    private String cteName;
    private Map<String, Union> unionMap = new LinkedHashMap<>();
    private Map<String, TableColumn<AliasRow, String>> unionColumns = new HashMap<>();

    private TableView<AliasRow> aliasTable = new TableView<>();
    private TableView<TableRow> unionTable = new TableView<>();

    private TreeTableView<TableRow> orderFieldsTree = new TreeTableView<>();
    private TableView<TableRow> orderTableResults = new TableView<>();
    private int curMaxUnion;

    public OneCte() {
        unionMap.put(UNION_0, new Union());
    }

    public OneCte(MainController controller) {
        this();
        aliasTable.getColumns().add(UnionAliases.aliasColumn());
        UnionAliases.addUnionColumn(aliasTable, unionTable, UNION_0, this, controller);
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
