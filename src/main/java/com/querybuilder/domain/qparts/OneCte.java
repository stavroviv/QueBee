package com.querybuilder.domain.qparts;

import com.querybuilder.domain.AliasRow;
import com.querybuilder.domain.TableRow;
import com.querybuilder.querypart.UnionAliases;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeTableView;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.querybuilder.utils.Constants.UNION_0;

@Data
public class OneCte {
    private String cteName;
    private Map<String, Union> unionMap = new LinkedHashMap<>();

    private TableView<AliasRow> aliasTable = new TableView<>();
    private TableView<TableRow> unionTable;

    private TreeTableView<TableRow> orderFieldsTree = new TreeTableView<>();
    private TableView<TableRow> orderTableResults = new TableView<>();
    private int curMaxUnion;

    public OneCte() {
        unionMap.put(UNION_0, new Union());
    }

    public void saveAliasTable(UnionAliases controller) {
        aliasTable.getItems().clear();
        TableView<AliasRow> newAliasTable = controller.getAliasTable();
        aliasTable.getColumns().clear();
        for (TableColumn<AliasRow, ?> column : newAliasTable.getColumns()) {
            aliasTable.getColumns().add(column);
        }
        aliasTable.getItems().addAll(controller.getAliasTable().getItems());

        unionTable.getItems().clear();
        unionTable.getItems().addAll(controller.getUnionTable().getItems());
    }

    public void showAliasTable(UnionAliases controller) {
        controller.getAliasTable().getItems().clear();
        controller.getAliasTable().getColumns().clear();
        for (TableColumn<AliasRow, ?> column : aliasTable.getColumns()) {
            controller.getAliasTable().getColumns().add(column);
        }
        controller.getAliasTable().getItems().addAll(aliasTable.getItems());

        controller.getUnionTable().getItems().clear();
        controller.getUnionTable().getItems().addAll(unionTable.getItems());
    }

}
