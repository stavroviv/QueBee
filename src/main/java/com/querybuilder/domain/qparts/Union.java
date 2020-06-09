package com.querybuilder.domain.qparts;

import com.querybuilder.domain.ConditionElement;
import com.querybuilder.domain.LinkElement;
import com.querybuilder.domain.TableRow;
import com.querybuilder.querypart.FromTables;
import com.querybuilder.querypart.GroupBy;
import com.querybuilder.querypart.Links;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import lombok.Data;

@Data
public class Union {
    private TreeTableView<TableRow> tablesView = new TreeTableView<>();
    private TableView<TableRow> fieldTable = new TableView<>();

    private TableView<LinkElement> linkTable = new TableView<>();

    private TreeTableView<TableRow> groupFieldsTree = new TreeTableView<>();
    private TableView<TableRow> groupTableResults = new TableView<>();
    private TableView<TableRow> groupTableAggregates = new TableView<>();

    private TreeTableView<TableRow> conditionsTreeTable = new TreeTableView<>();
    private TableView<ConditionElement> conditionTableResults = new TableView<>();

    public Union() {
        tablesView.setRoot(new TreeItem<>());
    }

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

    public void saveGroupBy(GroupBy controller) {
        groupTableResults.getItems().clear();
        groupTableResults.getItems().addAll(controller.getGroupTableResults().getItems());
        groupTableAggregates.getItems().clear();
        groupTableAggregates.getItems().addAll(controller.getGroupTableAggregates().getItems());
    }

    public void showGroupBy(GroupBy controller) {
        controller.getGroupTableResults().getItems().clear();
        controller.getGroupTableResults().getItems().addAll(groupTableResults.getItems());
        controller.getGroupTableAggregates().getItems().clear();
        controller.getGroupTableAggregates().getItems().addAll(groupTableAggregates.getItems());
    }
}
