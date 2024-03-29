package com.querybuilder.querypart;

import com.querybuilder.controllers.MainController;
import com.querybuilder.domain.AliasCell;
import com.querybuilder.domain.AliasRow;
import com.querybuilder.domain.TableRow;
import com.querybuilder.domain.qparts.OneCte;
import com.querybuilder.domain.qparts.Union;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.querybuilder.utils.Constants.*;
import static com.querybuilder.utils.Utils.*;

@Data
@EqualsAndHashCode(callSuper = false)
public class UnionAliases extends AbstractQueryPart {
    private static final String FIRST_COLUMN = "UNION_0";

    @FXML
    private TableView<AliasRow> aliasTable;
    @FXML
    private TableView<TableRow> unionTable;
    @FXML
    private TableColumn<TableRow, String> unionTableNameColumn;
    @FXML
    private TableColumn<TableRow, Boolean> unionTableDistinctColumn;

    @FXML
    @Override
    public void initialize() {
        aliasTable.getSelectionModel().cellSelectionEnabledProperty().set(true);

        unionTableNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));

        unionTableDistinctColumn.setCellFactory(CheckBoxTableCell.forTableColumn(unionTableDistinctColumn));
        unionTableDistinctColumn.setCellValueFactory(new PropertyValueFactory<>("distinct"));

        setUpDownBind(aliasTable, aliasUp, aliasDown);
        setUpDownBind(unionTable, unionUp, unionDown);
    }

    public static TableColumn<AliasRow, String> aliasColumn() {
        TableColumn<AliasRow, String> aliasFieldColumn = new TableColumn<>();
        aliasFieldColumn.setCellValueFactory(new PropertyValueFactory<>("alias"));
        aliasFieldColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        aliasFieldColumn.setEditable(true);
        aliasFieldColumn.setText("Alias");
        aliasFieldColumn.setMaxWidth(304.0);
        aliasFieldColumn.setMinWidth(200.0);
        aliasFieldColumn.setPrefWidth(304.0);

        aliasFieldColumn.setSortable(false);

        // ввод данных по кнопке Enter - весьма странно что переход в другую ячейку это не ввод
        aliasFieldColumn.setOnEditCommit((TableColumn.CellEditEvent<AliasRow, String> event) -> {
            TablePosition<AliasRow, String> pos = event.getTablePosition();
            String newFullName = event.getNewValue();
            int row = pos.getRow();
            AliasRow alias = event.getTableView().getItems().get(row);
            alias.setAlias(newFullName);
        });
        return aliasFieldColumn;
    }

    public void loadAliases(SelectBody selectBody, OneCte oneCte) {
        TableView<TableRow> unionTable = new TableView<>();

        TableView<AliasRow> aliasTable = new TableView<>();
        int curMaxUnion = 0;

        aliasTable.getColumns().add(aliasColumn());
        int i = 0;

        if (selectBody instanceof SetOperationList) { // UNION
            SetOperationList setOperationList = (SetOperationList) selectBody;
            curMaxUnion = setOperationList.getSelects().size();
            for (SelectBody sBody : setOperationList.getSelects()) {
                String unionName = "UNION_" + i;
                addUnionColumn(aliasTable, unionTable, unionName, oneCte, mainController, getUnionType(setOperationList, i));
                addAliasColumn(aliasTable, (PlainSelect) sBody, oneCte, unionName, i);
                i++;
            }
        } else if (selectBody instanceof PlainSelect || selectBody == null) { // ONE QUERY
            addUnionColumn(aliasTable, unionTable, FIRST_COLUMN, oneCte, mainController, false);
            PlainSelect pSelect = (PlainSelect) selectBody;
            if (selectBody != null) {
                addAliasColumn(aliasTable, pSelect, oneCte, FIRST_COLUMN, i);
            }
        }

        oneCte.setAliasTable(aliasTable);
        oneCte.setCurMaxUnion(curMaxUnion);
        oneCte.setUnionTable(unionTable);
    }

    private boolean getUnionType(SetOperationList setOperationList, int i) {
        if (i == 0) {
            return false;
        }
        List<SetOperation> operations = setOperationList.getOperations();
        SetOperation setOperation = operations.get(i - 1);
        return !setOperation.toString().contains("ALL");
    }

    private void addAliasColumn(TableView<AliasRow> aliasTable, PlainSelect pSelect, OneCte oneCte, String unionName, Integer unionOrder) {
        TableView<TableRow> groupTableAggregates = new TableView<>();
        TableView<TableRow> fieldTable = new TableView<>();

        List<SelectItem> selectItems = pSelect.getSelectItems();
        if (selectItems == null) {
            return;
        }
        int index = 0;
        for (SelectItem sItem : selectItems) {
            SelectExpressionItem select = (SelectExpressionItem) sItem;
            Alias alias = select.getAlias();
            Expression expression = select.getExpression();

            String expr = expression.toString();
            Function function = null;
            if (expression instanceof Column) {
                expr = getStringAndSetTableName(pSelect, (Column) expression);
            } else if (expression instanceof Function) {
                function = (Function) expression;
                if (function.getParameters().getExpressions().size() == 1) {
                    expression = function.getParameters().getExpressions().get(0);
                    expr = getStringAndSetTableName(pSelect, (Column) expression);
                }
            }
            String strAlias = alias != null ? select.getAlias().getName() : expr;

            String nameFromColumn = expression.toString();
            if (expression instanceof Column) {
                nameFromColumn = getNameFromColumn((Column) expression);
            }

            AliasRow aliasRow;
            if (unionName.equals(UNION_0)) {
                aliasRow = new AliasRow(expression.toString(), strAlias);
            } else {
                if (index > aliasTable.getItems().size()) {
                    throw new RuntimeException("Different column numbers in union");
                }
                aliasRow = aliasTable.getItems().get(index);
            }

            if (nameFromColumn.equals(NULL_VALUE)) {
                aliasRow.getValues().put(unionName, EMPTY_UNION_VALUE);
            } else {
                aliasRow.getValues().put(unionName, nameFromColumn);
            }

            if (!nameFromColumn.equals(NULL_VALUE)) {
                TableRow newField = new TableRow(nameFromColumn);
                aliasRow.getIds().put(unionName, newField.getId());

                fieldTable.getItems().add(newField);

                if (function != null) {
                    TableRow tableRow = TableRow.tableRowFromValue(newField);
                    tableRow.setComboBoxValue(function.getName());
                    groupTableAggregates.getItems().add(tableRow);
                }
            }

            if (unionName.equals(UNION_0)) {
                aliasTable.getItems().add(aliasRow);
            }

            index++;
        }

        Union union = oneCte.getUnionMap().get(unionName);
        if (union != null) {
            union.setFieldTable(fieldTable);
            union.setGroupTableAggregates(groupTableAggregates);
        } else {
            Union value = new Union(unionOrder);
            value.setFieldTable(fieldTable);
            value.setGroupTableAggregates(groupTableAggregates);
            oneCte.getUnionMap().put(unionName, value);
        }

    }

    private String getStringAndSetTableName(PlainSelect pSelect, Column expression) {
        String expr;
        expr = expression.getColumnName();
        if (expression.getTable() == null && pSelect.getJoins() == null) {
            expression.setTable((Table) pSelect.getFromItem());
        }
        return expr;
    }

    public static void addUnionColumn(TableView<AliasRow> aliasTable, TableView<TableRow> unionTable,
                                      String unionName, OneCte oneCte,
                                      MainController controller,
                                      boolean distinct) {
        TableColumn<AliasRow, String> newColumn = new TableColumn<>(unionName);
        newColumn.setEditable(true);
        for (AliasRow item : aliasTable.getItems()) {
            item.getValues().put(unionName, EMPTY_UNION_VALUE);
        }
        newColumn.setCellValueFactory(data -> {
            Map<String, String> values = data.getValue().getValues();
            String initialValue = values.get(unionName);
            return new SimpleStringProperty(initialValue);
        });

        newColumn.setCellFactory(x -> new AliasCell(x, unionName, controller));

        aliasTable.getSelectionModel().selectedIndexProperty().addListener((num) -> {
            TablePosition focusedCell = aliasTable.getFocusModel().getFocusedCell();
            aliasTable.edit(focusedCell.getRow(), focusedCell.getTableColumn());
        });

        aliasTable.getColumns().add(newColumn);
        oneCte.getUnionColumns().put(unionName, newColumn);

        TableRow unionRow = new TableRow(unionName);
        unionRow.setDistinct(distinct);
        unionTable.getItems().add(unionRow);
    }

    @FXML
    public void addNewUnion(ActionEvent event) {
        OneCte cte = mainController.getCurrentCte();

        int curMaxUnion = cte.getCurMaxUnion() + 1;
        cte.setCurMaxUnion(curMaxUnion);
        String key = "UNION_" + cte.getCurMaxUnion();

        cte.getUnionMap().put(key, new Union(curMaxUnion));

        String unionName = "Query " + cte.getCurMaxUnion();
        Tab tab = mainController.addUnionTabPane(unionName, key);
        activateNewTab(tab, mainController.getUnionTabPane(), mainController);

        addUnionColumn(aliasTable, unionTable, key, cte, mainController, false);
    }

    @FXML
    public void deleteUnion(ActionEvent event) {
        if (unionTable.getItems().size() == 1) {
            return;
        }

        mainController.setWithoutSave(true);

        OneCte currentCte = mainController.getCurrentCte();

        TableRow selectedItem = unionTable.getSelectionModel().getSelectedItem();
        String delUnion = selectedItem.getName();
        aliasTable.getColumns().remove(currentCte.getUnionColumns().get(delUnion));
        unionTable.getItems().remove(selectedItem);
        currentCte.getUnionColumns().remove(delUnion);
        currentCte.getUnionMap().remove(delUnion);
        for (AliasRow item : aliasTable.getItems()) {
            item.getValues().remove(delUnion);
        }

        int delIndex = getTabIndex(mainController.getUnionTabPane(), delUnion);

        TabPane unionTabPane = mainController.getUnionTabPane();
        int selectedIndex = mainController.getUnionTabPane().getSelectionModel().getSelectedIndex();
        unionTabPane.getTabs().remove(delIndex);

        if (selectedIndex == delIndex) {
            unionTabPane.getSelectionModel().select(delIndex - 1);
        }

        mainController.setWithoutSave(false);

        removeEmptyAliases(mainController);
    }

    public void addAlias(TableRow newField) {
        String name = newField.getName();
        String alias = name.split("\\.")[1];// TODO переделать на парсер выражений
        AliasRow aliasRow = new AliasRow(name, alias);
        aliasRow.setId(newField.getId());

        SingleSelectionModel<Tab> currentTab = mainController.getUnionTabPane().getSelectionModel();
        String curUnion = currentTab.getSelectedItem() == null ? FIRST_COLUMN : currentTab.getSelectedItem().getId();

        boolean exists = false;
        boolean rename = false;
        for (AliasRow item : aliasTable.getItems()) {
            if (!item.getAlias().equals(alias)) {
                continue;
            }
            String curVal = item.getValues().get(curUnion);
            if (curVal != null && !EMPTY_UNION_VALUE.equals(curVal)) {
                rename = true;
                break;
            }
            item.getValues().put(curUnion, name);
            exists = true;
            break;
        }

        OneCte cte = mainController.getCurrentCte();
        if (!exists) {
            for (String column : cte.getUnionColumns().keySet()) {
                aliasRow.getValues().put(column, column.equals(curUnion) ? name : EMPTY_UNION_VALUE);
            }
            if (rename) {
                aliasRow.setAlias(aliasRow.getAlias() + getCount(aliasRow.getAlias()));
            }
            aliasTable.getItems().add(aliasRow);
        }
        aliasTable.refresh();
    }

    private String getCount(String alias) {
        int count = 0;
        for (AliasRow item : aliasTable.getItems()) {
            if (item.getAlias().contains(alias)) {
                count++;
            }
        }
        return "" + count;
    }

    public void deleteAlias(TableRow x) {
        AliasRow deleted = null;
        for (AliasRow item : aliasTable.getItems()) {
            if (item.getId() == x.getId()) {
                deleted = item;
                break;
            }
        }
        aliasTable.getItems().remove(deleted);
    }

    @FXML
    private Button aliasUp;
    @FXML
    private Button aliasDown;
    @FXML
    private Button unionUp;
    @FXML
    private Button unionDown;

    @FXML
    public void unionUpClick(ActionEvent actionEvent) {
        moveUnion(-1);
    }

    @FXML
    public void unionDownClick(ActionEvent actionEvent) {
        moveUnion(1);
    }

    public void moveUnion(int upDown) {
        moveAliasColumn(unionTable, aliasTable, upDown);
        changeUnionOrder(mainController, unionTable, upDown);
        moveTab(mainController.getUnionTabPane(), unionTable, upDown);
        moveRow(unionTable, upDown);
    }

    private static void moveAliasColumn(TableView<TableRow> unionTable, TableView<AliasRow> aliasTable, int move) {
        TableRow selectedItem = unionTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            return;
        }
        TableColumn<AliasRow, ?> current = null;
        int index = 0;
        for (TableColumn<AliasRow, ?> column : aliasTable.getColumns()) {
            if (column.getText().equals(selectedItem.getName())) {
                current = column;
                break;
            }
            index++;
        }
        aliasTable.getColumns().remove(current);
        aliasTable.getColumns().add(index + move, current);
        aliasTable.refresh();
    }

    @FXML
    public void aliasUpClick(ActionEvent actionEvent) {
        moveRowUp(aliasTable);
    }

    @FXML
    public void aliasDownClick(ActionEvent actionEvent) {
        moveRowDown(aliasTable);
    }

    @FXML
    public void deleteAliasClick(ActionEvent actionEvent) {
        TableView.TableViewSelectionModel<AliasRow> selectionModel = aliasTable.getSelectionModel();
        AliasRow selectedItem = selectionModel.getSelectedItem();

        for (Map.Entry<String, Long> idEntry : selectedItem.getIds().entrySet()) {
            for (Map.Entry<String, Union> stringUnionEntry : mainController.getCurrentCte().getUnionMap().entrySet()) {
                Union union = stringUnionEntry.getValue();
                List<TableRow> toDelete = new ArrayList<>();
                for (TableRow item : union.getFieldTable().getItems()) {
                    if (item.getId() != idEntry.getValue()) {
                        continue;
                    }
                    toDelete.add(item);
                }
                for (TableRow tableRow : toDelete) {
                    union.getFieldTable().getItems().remove(tableRow);
                }
            }
        }

        int index = selectionModel.getSelectedIndex();
        aliasTable.getItems().remove(index);

        mainController.saveCurrent();
    }

}
