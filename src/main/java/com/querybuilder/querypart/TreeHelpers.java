package com.querybuilder.querypart;

import com.querybuilder.controllers.MainController;
import com.querybuilder.domain.SelectedFieldsTree;
import com.querybuilder.domain.TableRow;
import com.querybuilder.domain.qparts.Union;
import com.querybuilder.eventbus.CustomEvent;
import com.querybuilder.eventbus.CustomEventBus;
import javafx.collections.ListChangeListener;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;

import java.util.ArrayList;
import java.util.List;

import static com.querybuilder.domain.ConditionCell.REFRESH_SELECTED_TREE;
import static com.querybuilder.utils.Utils.applyChange;

public class TreeHelpers {

    public static void load(MainController controller, Union union) {
        TreeTableView<TableRow> tablesView = controller.getTableFieldsController().getTablesView();
        TreeTableView<TableRow> groupFieldsTree = controller.getGroupingController().getGroupFieldsTree();
        TreeTableView<TableRow> orderFieldsTree = controller.getOrderController().getOrderFieldsTree();
        TableView<TableRow> fieldTable = controller.getTableFieldsController().getFieldTable();
        TreeTableView<TableRow> conditionsTreeTable = controller.getConditionsController().getConditionsTreeTable();

        SelectedFieldsTree selectedGroupFieldsTree = new SelectedFieldsTree(tablesView, groupFieldsTree, fieldTable);
        controller.setSelectedGroupFieldsTree(selectedGroupFieldsTree);
        controller.setSelectedOrderFieldsTree(new SelectedFieldsTree(tablesView, orderFieldsTree, fieldTable));
        controller.setSelectedConditionsTreeTable(new SelectedFieldsTree(tablesView, conditionsTreeTable));

        union.getGroupFieldsTree().setRoot(selectedGroupFieldsTree);

        // так как корень перезагружается
        tablesView.getRoot().getChildren().addListener((ListChangeListener<TreeItem<TableRow>>) change -> {
            while (change.next()) {
                List<SelectedFieldsTree> selectedFieldTrees = new ArrayList<>();
                selectedFieldTrees.add(controller.getSelectedGroupFieldsTree());
                selectedFieldTrees.add(controller.getSelectedConditionsTreeTable());
                selectedFieldTrees.add(controller.getSelectedOrderFieldsTree());
                applyChange(selectedFieldTrees, selectedFieldsTree -> selectedFieldsTree.applyChanges(change));

                CustomEvent customEvent = new CustomEvent();
                customEvent.setName(REFRESH_SELECTED_TREE);
                customEvent.setChange(change);
                CustomEventBus.post(customEvent);
            }
        });
    }

    public static void cleanTrees(Union union) {
        for (TableRow item : union.getGroupTableResults().getItems()) {
            TreeItem<TableRow> root = union.getGroupFieldsTree().getRoot();
            root.getChildren().removeIf(ddd -> ddd.getValue().getName().equals(item.getName()));
        }
        for (TableRow item : union.getGroupTableAggregates().getItems()) {
            TreeItem<TableRow> root = union.getGroupFieldsTree().getRoot();
            // TODO переделать на ID
            root.getChildren().removeIf(ddd -> ddd.getValue().getName().equals(item.getName()));
        }
    }
}
