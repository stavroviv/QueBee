package com.querybuilder.domain.qparts;

import com.querybuilder.domain.ConditionElement;
import com.querybuilder.domain.LinkElement;
import com.querybuilder.domain.TableRow;
import com.querybuilder.querypart.FromTables;
import com.querybuilder.querypart.Links;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeTableView;
import lombok.Data;

@Data
public class Union {
    private TreeTableView<TableRow> tablesView;
    private TableView<TableRow> fieldTable;

    private TableView<LinkElement> linkTable;

    private TreeTableView<TableRow> groupFieldsTree;
    private TableView<TableRow> groupTableResults;
    private TableView<TableRow> groupTableAggregates;

    private TreeTableView<TableRow> conditionsTreeTable;
    private TableView<ConditionElement> conditionTableResults;

    public void saveFrom(FromTables controller) {
        fieldTable.getItems().clear();
        fieldTable.getItems().addAll(controller.getFieldTable().getItems());
        tablesView.setRoot(controller.getTablesView().getRoot());
    }

    public void showFrom(FromTables controller) {
        controller.getFieldTable().getItems().clear();
        controller.getFieldTable().getItems().addAll(fieldTable.getItems());
        controller.getTablesView().setRoot(tablesView.getRoot());
    }

    public void saveLink(Links controller) {
        linkTable.getItems().clear();
        linkTable.getItems().addAll(controller.getLinkTable().getItems());
    }

    public void showLinks(Links controller) {
        controller.getLinkTable().getItems().clear();
        controller.getLinkTable().getItems().addAll(linkTable.getItems());
    }
}
