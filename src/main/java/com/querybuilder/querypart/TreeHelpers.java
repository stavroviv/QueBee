package com.querybuilder.querypart;

import com.querybuilder.controllers.MainController;
import com.querybuilder.domain.SelectedFieldsTree;
import com.querybuilder.domain.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeTableView;

public class TreeHelpers {

    public static void load(MainController controller) {
        TreeTableView<TableRow> tablesView = controller.getTableFieldsController().getTablesView();
        TreeTableView<TableRow> groupFieldsTree = controller.getGroupingController().getGroupFieldsTree();
        TreeTableView<TableRow> orderFieldsTree = controller.getOrderController().getOrderFieldsTree();
        TableView<TableRow> fieldTable = controller.getTableFieldsController().getFieldTable();
        TreeTableView<TableRow> conditionsTreeTable = controller.getConditionsController().getConditionsTreeTable();

        controller.setSelectedGroupFieldsTree(new SelectedFieldsTree(tablesView, groupFieldsTree, fieldTable));
        controller.setSelectedOrderFieldsTree(new SelectedFieldsTree(tablesView, orderFieldsTree, fieldTable));
        controller.setSelectedConditionsTreeTable(new SelectedFieldsTree(tablesView, conditionsTreeTable));
    }
}
