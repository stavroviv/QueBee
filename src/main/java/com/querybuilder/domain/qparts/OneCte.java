package com.querybuilder.domain.qparts;

import com.querybuilder.domain.AliasRow;
import com.querybuilder.domain.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeTableView;
import lombok.Data;

import java.util.Map;

@Data
public class OneCte {
    private String cteName;
    private Map<String, Union> unionMap;

    private TableView<AliasRow> aliasTable;
    private TreeTableView<TableRow> orderFieldsTree;
    private TableView<TableRow> orderTableResults;

//    public void setTablesView(TreeTableView<TableRow> tablesView) {
//        this.tablesView = tablesView;
//    }
//
//    public void setFieldTable(TableView<TableRow> fieldTable) {
//        this.fieldTable = new TableView<>();
//        this.fieldTable.getItems().addAll(
//                FXCollections.observableArrayList(fieldTable.getItems())
//        );
//    }
}
