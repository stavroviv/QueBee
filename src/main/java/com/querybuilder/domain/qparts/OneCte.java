package com.querybuilder.domain.qparts;

import com.querybuilder.domain.AliasRow;
import com.querybuilder.domain.TableRow;
import com.querybuilder.querypart.UnionAliases;
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
    private TreeTableView<TableRow> orderFieldsTree = new TreeTableView<>();
    private TableView<TableRow> orderTableResults = new TableView<>();

    public OneCte() {
        unionMap.put(UNION_0, new Union());
    }

    public void saveAliasTable(UnionAliases controller) {
        aliasTable.getItems().clear();
        aliasTable.getItems().addAll(controller.getAliasTable().getItems());
    }

    public void showAliasTable(UnionAliases controller) {
        controller.getAliasTable().getItems().clear();
        controller.getAliasTable().getItems().addAll(aliasTable.getItems());
    }

}
