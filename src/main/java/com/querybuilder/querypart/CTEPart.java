package com.querybuilder.querypart;

import com.querybuilder.controllers.MainController;
import com.querybuilder.domain.*;
import com.querybuilder.domain.qparts.OneCte;
import com.querybuilder.domain.qparts.Union;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TreeItem;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;

import java.util.Iterator;
import java.util.Map;

import static com.querybuilder.utils.Constants.CTE_ROOT;
import static com.querybuilder.utils.Utils.showErrorMessage;

public class CTEPart {

    public static void init(MainController controller) {
        controller.getQueryCteColumn().setCellFactory(TextFieldTableCell.forTableColumn());
        controller.getQueryCteColumn().setCellValueFactory(new PropertyValueFactory<>("name"));
        controller.getQueryCteColumn().setEditable(true);
        controller.getQueryCteColumn().setOnEditCommit(
                (TableColumn.CellEditEvent<CteRow, String> event) -> setNewCteName(controller, event)
        );

        ReadOnlyIntegerProperty selectedIndex = controller.getQueryCteTable().getSelectionModel().selectedIndexProperty();
        controller.getCteUpButton().disableProperty().bind(selectedIndex.lessThanOrEqualTo(0));
        controller.getCteDownButton().disableProperty().bind(Bindings.createBooleanBinding(() -> {
                    int index = selectedIndex.get();
                    return index < 0 || index + 1 >= controller.getQueryCteTable().getItems().size();
                },
                selectedIndex, controller.getQueryCteTable().getItems())
        );
    }

    private static void setNewCteName(MainController controller, TableColumn.CellEditEvent<CteRow, String> event) {
        TablePosition<CteRow, String> pos = event.getTablePosition();

        ObservableList<CteRow> items = event.getTableView().getItems();
        CteRow cteRow = items.get(pos.getRow());
        String newValue = event.getNewValue();

        try {
            checkNewName(controller, newValue, cteRow);
            setNewName(controller, cteRow, event.getOldValue(), newValue);
        } catch (Exception e) {
            showErrorMessage(e.getMessage(), "Attention");
            cteRow.setName(event.getOldValue());
            controller.getQueryCteTable().refresh();
        }
    }

    public static void checkNewName(MainController controller, String newValue, CteRow cteRow) {
        ObservableList<CteRow> items = controller.getQueryCteTable().getItems();
        for (CteRow item : items) {
            if (item.getName().equals(newValue) && !item.getId().equals(cteRow.getId())) {
                throw new RuntimeException("Duplicate cte name");
            }
        }

        try {
            CCJSqlParserUtil.parse("SELECT * FROM " + newValue);
        } catch (Exception e) {
            throw new RuntimeException("Incorrect new name: " + newValue);
        }
    }

    public static void setNewName(MainController controller, CteRow cteRow, String oldValue, String newValue) {

        cteRow.setName(newValue);
        Map<String, OneCte> cteMap = controller.getFullQuery().getCteMap();
        cteMap.get(cteRow.getId()).setCteName(newValue);
        for (Tab tPane : controller.getCteTabPane().getTabs()) {
            if (tPane.getId().equals(cteRow.getId())) {
                tPane.setText(newValue);
                break;
            }
        }

        for (Map.Entry<String, OneCte> oneCteEntry : cteMap.entrySet()) {
            OneCte oneCte = oneCteEntry.getValue();
            for (Map.Entry<String, Union> unionEntry : oneCte.getUnionMap().entrySet()) {
                Union union = unionEntry.getValue();
                for (TreeItem<TableRow> child : union.getTablesView().getRoot().getChildren()) {
                    if (child.getValue().getName().equals(oldValue)) {
                        child.getValue().setName(newValue);
                    }
                }
                for (TableRow item : union.getFieldTable().getItems()) {
                    if (item.getName().contains(oldValue + ".")) {
                        item.setName(item.getName().replace(oldValue, newValue));
                    }
                }
                for (LinkElement item : union.getLinkTable().getItems()) {
                    if (item.getTable1().equals(oldValue)) {
                        item.setTable1(newValue);
                    }
                    if (item.getTable2().equals(oldValue)) {
                        item.setTable2(newValue);
                    }
                    if (item.getCondition().contains(oldValue)) {
                        item.setCondition(item.getCondition().replace(oldValue, newValue));
                    }
                }
                for (ConditionElement item : union.getConditionTableResults().getItems()) {
                    if (item.getCondition().contains(oldValue + ".")) {
                        item.setCondition(item.getCondition().replace(oldValue, newValue));
                    }
                }
                for (TableRow item : union.getGroupTableResults().getItems()) {
                    if (item.getName().contains(oldValue + ".")) {
                        item.setName(item.getName().replace(oldValue, newValue));
                    }
                }
            }
            for (AliasRow item : oneCte.getAliasTable().getItems()) {
                for (Map.Entry<String, String> aliasEntry : item.getValues().entrySet()) {
                    if (aliasEntry.getValue().contains(oldValue + ".")) {
                        aliasEntry.setValue(aliasEntry.getValue().replace(oldValue, newValue));
                    }
                }
            }
            for (TableRow item : oneCte.getOrderTableResults().getItems()) {
                if (item.getName().contains(oldValue + ".")) {
                    item.setName(item.getName().replace(oldValue, newValue));
                }
            }
        }
    }

    public static void load(MainController controller) {
        // загрузить в дерево таблиц предыдущие CTE
        TreeItem<TableRow> root = controller.getTableFieldsController().getDatabaseTableView().getRoot();
        root.getChildren().forEach(item -> {
            if (item.getValue().isCte()) {
                root.getChildren().remove(item);
            }
        });

        if (controller.getFullQuery().getCteMap().size() == 1) {
            return;
        }

        int currentCTE = controller.getCteTabPane().getSelectionModel().getSelectedIndex();
        if (currentCTE == 0) {
            return;
        }

        TableRow cteRoot = new TableRow(CTE_ROOT);
        cteRoot.setCteRoot(true);
        cteRoot.setCte(true);
        TreeItem<TableRow> cteRootItem = new TreeItem<>(cteRoot);
        cteRootItem.setExpanded(true);
        root.getChildren().add(0, cteRootItem);

        Iterator<String> iterator = controller.getFullQuery().getCteMap().keySet().iterator();
        String currentCte = controller.getCteTabPane().getSelectionModel().getSelectedItem().getId();
        while (iterator.hasNext()) {
            String cteId = iterator.next();
            if (currentCte.equals(cteId)) {
                break;
            }
            OneCte oneCte = controller.getFullQuery().getCteMap().get(cteId);
            TableRow tableRow = new TableRow(oneCte.getCteName());
            tableRow.setRoot(true);
            tableRow.setCte(true);
            TreeItem<TableRow> treeItem = new TreeItem<>(tableRow);
            cteRootItem.getChildren().add(treeItem);


//            if (withItem.getSelectBody() instanceof PlainSelect) {
//                PlainSelect selectBody = (PlainSelect) withItem.getSelectBody();
//                List<SelectItem> selectItems = selectBody.getSelectItems();
            oneCte.getAliasTable().getItems().forEach(item -> {
//                    SelectExpressionItem selectItem = (SelectExpressionItem) item;
//                    String name;
//                    if (selectItem.getAlias() != null) {
//                        name = selectItem.getAlias().getName();
//                    } else {
//                        Column column = (Column) selectItem.getExpression();
//                        String[] split = column.getColumnName().split("\\.");
//                        name = split.length > 1 ? split[1] : split[0]; // FIXME
//                    }
                treeItem.getChildren().add(new TreeItem<>(new TableRow(item.getAlias())));
            });
//            }

        }
    }
}
