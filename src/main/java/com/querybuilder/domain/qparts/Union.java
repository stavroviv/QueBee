package com.querybuilder.domain.qparts;

import com.querybuilder.domain.ConditionElement;
import com.querybuilder.domain.LinkElement;
import com.querybuilder.domain.TableRow;
import com.querybuilder.querypart.Conditions;
import com.querybuilder.querypart.FromTables;
import com.querybuilder.querypart.GroupBy;
import com.querybuilder.querypart.Links;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import lombok.Data;

import static com.querybuilder.utils.Utils.loadTableToTable;

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
        loadTableToTable(controller.getFieldTable(), fieldTable);
        tablesView.setRoot(controller.getTablesView().getRoot());
    }

    public void showFrom(FromTables controller) {
        loadTableToTable(fieldTable, controller.getFieldTable());
        controller.getTablesView().setRoot(tablesView.getRoot());
    }

    public void saveLink(Links controller) {
        loadTableToTable(controller.getLinkTable(), linkTable);
    }

    public void showLinks(Links controller) {
        loadTableToTable(linkTable, controller.getLinkTable());
    }

    public void saveGroupBy(GroupBy controller) {
        loadTableToTable(controller.getGroupTableResults(), groupTableResults);
        loadTableToTable(controller.getGroupTableAggregates(), groupTableAggregates);
    }

    public void showGroupBy(GroupBy controller) {
        loadTableToTable(groupTableResults, controller.getGroupTableResults());
        loadTableToTable(groupTableAggregates, controller.getGroupTableAggregates());
    }

    public void saveConditions(Conditions controller) {
        loadTableToTable(controller.getConditionTableResults(), conditionTableResults);
    }

    public void showConditions(Conditions controller) {
        loadTableToTable(conditionTableResults, controller.getConditionTableResults());
    }

}
