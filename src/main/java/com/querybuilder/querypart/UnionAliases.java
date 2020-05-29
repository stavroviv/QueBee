package com.querybuilder.querypart;

import com.querybuilder.controllers.MainController;
import com.querybuilder.domain.AliasCell;
import com.querybuilder.domain.AliasRow;
import com.querybuilder.domain.TableRow;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.statement.select.*;

import java.util.ArrayList;
import java.util.List;

import static com.querybuilder.utils.Utils.showMessage;

public class UnionAliases {

    public static void init(MainController controller) {

        controller.getAliasTable().getSelectionModel().cellSelectionEnabledProperty().set(true);

        controller.getAliasFieldColumn().setCellValueFactory(new PropertyValueFactory<>("alias"));
        controller.getAliasFieldColumn().setCellFactory(TextFieldTableCell.forTableColumn());
        controller.getAliasFieldColumn().setOnEditCommit((TableColumn.CellEditEvent<AliasRow, String> event) -> {
            TablePosition<AliasRow, String> pos = event.getTablePosition();
            String newFullName = event.getNewValue();
            int row = pos.getRow();
            AliasRow person = event.getTableView().getItems().get(row);
            person.setAlias(newFullName);
        });

        controller.getUnionTableNameColumn().setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        controller.getUnionTableDistinctColumn().setCellFactory(tc -> new CheckBoxTableCell<>());
        controller.getUnionTableDistinctColumn().setCellValueFactory(data -> new SimpleBooleanProperty(data.getValue().isDistinct()));

    }

    public static void load(MainController controller, SelectBody selectBody) {
        TableView<AliasRow> aliasTable = controller.getAliasTable();

        aliasTable.getItems().clear();
        int size = aliasTable.getColumns().size();
        aliasTable.getColumns().remove(1, size);
        if (selectBody instanceof SetOperationList) { // UNION
            SetOperationList setOperationList = (SetOperationList) selectBody;
            int i = 1;
            for (SelectBody sBody : setOperationList.getSelects()) {
                addUnionColumn(controller, "Query " + i, i - 1);
                PlainSelect pSelect = (PlainSelect) sBody;
                if (i == 1) {
                    addAliasFirstColumn(controller, pSelect);
                } else {
                    int j = 0;
                    if (pSelect.getSelectItems().size() > aliasTable.getItems().size()) {
                        showMessage("Error: different number of fields in union queries");
                        throw new RuntimeException("Разное количество полей в объединяемых запросах");
                    }
                    for (Object x : pSelect.getSelectItems()) {
                        SelectExpressionItem select = (SelectExpressionItem) x;
                        Alias alias = select.getAlias();
                        String strAlias = alias != null ? select.getAlias().getName() : select.getExpression().toString();
                        AliasRow aliasEl = aliasTable.getItems().get(j);
                        aliasEl.getValues().add(strAlias);
                        j++;
                    }
                }
                i++;
            }
        } else if (selectBody instanceof PlainSelect || selectBody == null) { // ONE QUERY
            addUnionColumn(controller, "Query 1", 0);
            PlainSelect pSelect = (PlainSelect) selectBody;
            if (selectBody != null) {
                addAliasFirstColumn(controller, pSelect);
            }
        }
    }

    private static void addAliasFirstColumn(MainController controller, PlainSelect pSelect) {
        List<SelectItem> selectItems = pSelect.getSelectItems();
        if (selectItems == null) {
            return;
        }
        for (Object x : selectItems) {
            SelectExpressionItem select = (SelectExpressionItem) x;
            Alias alias = select.getAlias();
            String strAlias = alias != null ? select.getAlias().getName() : select.getExpression().toString();
            AliasRow aliasRow = new AliasRow(select.toString(), strAlias);
            aliasRow.getValues().add(strAlias);
            controller.getAliasTable().getItems().add(aliasRow);
        }
    }

    public static void addUnionColumn(MainController controller, String unionName, int i) {
        TableColumn<AliasRow, String> newColumn = new TableColumn<>(unionName);
        newColumn.setEditable(true);

        newColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getValues().get(i)));

        newColumn.setCellFactory(x -> {
            List<String> items = new ArrayList<>();
            controller.getFieldTable().getItems().forEach(x1 -> items.add(x1.getName()));
            return new AliasCell(x, i, items);
        });

        TableView<AliasRow> aliasTable = controller.getAliasTable();
        aliasTable.getSelectionModel().selectedIndexProperty().addListener((num) -> {
            TablePosition focusedCell = aliasTable.getFocusModel().getFocusedCell();
            aliasTable.edit(focusedCell.getRow(), focusedCell.getTableColumn());
        });

        aliasTable.getColumns().add(newColumn);
        controller.getUnionColumns().put(unionName, newColumn);
        controller.getUnionTable().getItems().add(new TableRow(unionName));
    }
}
