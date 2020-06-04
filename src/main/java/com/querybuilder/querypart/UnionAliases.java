package com.querybuilder.querypart;

import com.querybuilder.domain.AliasCell;
import com.querybuilder.domain.AliasRow;
import com.querybuilder.domain.TableRow;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
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
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.querybuilder.utils.Utils.*;

@Data
@EqualsAndHashCode(callSuper = false)
public class UnionAliases extends AbstractQueryPart {
    private int curMaxUnion; // индекс максимального объединения, нумерация начинается с 0
    private Map<String, TableColumn<AliasRow, String>> unionColumns;

    @FXML
    private TableView<AliasRow> aliasTable;
    @FXML
    private TableColumn<AliasRow, String> aliasFieldColumn;
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

        aliasFieldColumn.setCellValueFactory(new PropertyValueFactory<>("alias"));
        aliasFieldColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        aliasFieldColumn.setEditable(true);
        // ввод данных по кнопке Enter - весьма странно что переход в другую ячейку это не ввод
        aliasFieldColumn.setOnEditCommit((TableColumn.CellEditEvent<AliasRow, String> event) -> {
            TablePosition<AliasRow, String> pos = event.getTablePosition();
            String newFullName = event.getNewValue();
            int row = pos.getRow();
            AliasRow person = event.getTableView().getItems().get(row);
            person.setAlias(newFullName);
        });

        unionTableNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        unionTableDistinctColumn.setCellFactory(tc -> new CheckBoxTableCell<>());
        unionTableDistinctColumn.setCellValueFactory(data -> new SimpleBooleanProperty(data.getValue().isDistinct()));
    }

    @Override
    public void load(PlainSelect pSelect) {
        // custom load
    }

    @Override
    public void save(PlainSelect pSelect) {
        // custom save
    }

    public void saveAliases(PlainSelect pSelect) {
        List<SelectItem> selectItems = pSelect.getSelectItems();
        int i = 0;
        for (SelectItem selectItem : selectItems) {
            if (selectItem instanceof SelectExpressionItem) {
                Expression expression = ((SelectExpressionItem) selectItem).getExpression();
                if (expression instanceof Column) {
                    Column column = (Column) expression;
                    AliasRow aliasRow = aliasTable.getItems().get(i);
                    if (!column.getColumnName().equals(aliasRow.getAlias())) {
                        Alias alias = new Alias(aliasRow.getAlias(), true);
                        ((SelectExpressionItem) selectItem).setAlias(alias);
                    }
                }
            }
            i++;
        }
        pSelect.setSelectItems(selectItems);
    }

    public void loadAliases(SelectBody selectBody) {
        unionTable.getItems().clear();
        curMaxUnion = 0;
        unionColumns = new HashMap<>();
        aliasTable.getItems().clear();

        int size = aliasTable.getColumns().size();
        aliasTable.getColumns().remove(1, size);
        if (selectBody instanceof SetOperationList) { // UNION
            SetOperationList setOperationList = (SetOperationList) selectBody;
            int i = 1;
            for (SelectBody sBody : setOperationList.getSelects()) {
                addUnionColumn("Query " + i, i - 1);
                PlainSelect pSelect = (PlainSelect) sBody;
                if (i == 1) {
                    addAliasFirstColumn(pSelect);
                } else {
                    int j = 0;
                    if (pSelect.getSelectItems().size() > aliasTable.getItems().size()) {
                        showMessage("Error: different number of fields in union queries");
                        throw new RuntimeException("Разное количество полей в объединяемых запросах");
                    }
                    for (SelectItem x : pSelect.getSelectItems()) {
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
            addUnionColumn("Query 1", 0);
            PlainSelect pSelect = (PlainSelect) selectBody;
            if (selectBody != null) {
                addAliasFirstColumn(pSelect);
            }
        }
    }

    private void addAliasFirstColumn(PlainSelect pSelect) {
        List<SelectItem> selectItems = pSelect.getSelectItems();
        if (selectItems == null) {
            return;
        }
        for (SelectItem sItem : selectItems) {
            SelectExpressionItem select = (SelectExpressionItem) sItem;
            Alias alias = select.getAlias();
            Expression expression = select.getExpression();

            String expr = expression.toString();
            if (expression instanceof Column) {
                expr = ((Column) expression).getColumnName();
            }
            String strAlias = alias != null ? select.getAlias().getName() : expr;

            AliasRow aliasRow = new AliasRow(expression.toString(), strAlias);
            aliasRow.getValues().add(getNameFromColumn((Column) expression));
            aliasTable.getItems().add(aliasRow);
        }

        setAliasesIds();
    }

    public void setAliasesIds() {
        ObservableList<TableRow> items = mainController.getTableFieldsController().getFieldTable().getItems();
        int i = 0;
        for (TableRow item : items) {
            aliasTable.getItems().get(i).setId(item.getId());
            i++;
        }
    }

    public void addUnionColumn(String unionName, int i) {
        TableColumn<AliasRow, String> newColumn = new TableColumn<>(unionName);
        newColumn.setEditable(true);

        newColumn.setCellValueFactory(data -> {
            List<String> values = data.getValue().getValues();
            if (values.size() <= i) {
                return null;
            }
            String initialValue = values.get(i);
            return new SimpleStringProperty(initialValue);
        });

        newColumn.setCellFactory(x -> new AliasCell(x, i, mainController));

        aliasTable.getSelectionModel().selectedIndexProperty().addListener((num) -> {
            TablePosition focusedCell = aliasTable.getFocusModel().getFocusedCell();
            aliasTable.edit(focusedCell.getRow(), focusedCell.getTableColumn());
        });

        aliasTable.getColumns().add(newColumn);
        unionColumns.put(unionName, newColumn);
        unionTable.getItems().add(new TableRow(unionName));
    }

    @FXML
    protected void addUnionQuery(ActionEvent event) {
        curMaxUnion++;
        aliasTable.getItems().forEach(x -> x.getValues().add(""));

        addNewUnion();

        if (curMaxUnion == 1) {
            addFirstUnion();
            return;
        }

        String unionName = "Query " + (curMaxUnion + 1);
        addUnionColumn(unionName, curMaxUnion);
        addUnion(unionName, curMaxUnion);
    }

    private void addFirstUnion() {
        addUnion("Query 1", 0);
        addUnion("Query 2", 1);
        addUnionColumn("Query 2", curMaxUnion);
    }

    private void addNewUnion() {
        SetOperationList selectBody = new SetOperationList();

        List<SelectBody> selectBodies = new ArrayList<>();
        SelectBody existingBody = mainController.getFullSelectBody();
        if (existingBody instanceof SetOperationList) {
            SetOperationList setOperationList = (SetOperationList) existingBody;
            selectBodies.addAll(setOperationList.getSelects());
        } else {
            selectBodies.add(existingBody);
        }
        selectBodies.add(getEmptySelect());

        List<Boolean> brackets = new ArrayList<>();
        List<SetOperation> ops = new ArrayList<>();
        selectBodies.forEach(x -> {
            brackets.add(false);
            UnionOp unionOp = new UnionOp();
            unionOp.setAll(true);
            ops.add(unionOp);
        });
        ops.remove(ops.size() - 1);

        selectBody.setBracketsOpsAndSelects(brackets, selectBodies, ops);

        int cteNumber = mainController.getCteTabPane().getSelectionModel().getSelectedIndex();
        Select sQuery = mainController.getSQuery();
        if (sQuery.getWithItemsList() == null || cteNumber == sQuery.getWithItemsList().size()) {
            sQuery.setSelectBody(selectBody);
        } else {
            sQuery.getWithItemsList().get(cteNumber).setSelectBody(selectBody);
        }
    }

    public void addUnion(String unionName, int curUnion) {
        Tab tab = new Tab(unionName);
        tab.setId(unionName);
        mainController.getUnionTabPane().getTabs().add(tab);
    }

    @FXML
    protected void deleteUnion(ActionEvent event) {
        if (unionTable.getItems().size() == 1) {
            return;
        }

        mainController.setNotChangeUnion(true);
        TabPane unionTabPane = mainController.getUnionTabPane();
        int selectedIndex = unionTabPane.getSelectionModel().getSelectedIndex();

        TableRow selectedItem = unionTable.getSelectionModel().getSelectedItem();
        String name = selectedItem.getName();
        aliasTable.getColumns().remove(unionColumns.get(name));
        unionTable.getItems().remove(selectedItem);

        int delIndex = getTabIndex(name);
        SelectBody currentSelectBody = mainController.getSQuery().getSelectBody();
        ((SetOperationList) currentSelectBody).getSelects().remove(delIndex);
        unionTabPane.getTabs().remove(delIndex);
        if (selectedIndex == delIndex) {
            unionTabPane.getSelectionModel().select(delIndex - 1);
            mainController.loadCurrentQuery(false);
        }

        mainController.setNotChangeUnion(false);
    }

    public int getTabIndex(String unionTabId) {
        int tIndex = 0;
        for (Tab tPane : mainController.getUnionTabPane().getTabs()) {
            if (tPane.getId().equals(unionTabId)) {
                break;
            }
            tIndex++;
        }
        return tIndex;
    }

    public void addAlias(TableRow newField) {
        String name = newField.getName();
        String alias = name.split("\\.")[1];// TODO переделать на парсер выражений
        AliasRow aliasRow = new AliasRow(name, alias);
        aliasRow.setId(newField.getId());

        int size = mainController.getUnionTabPane().getTabs().size();
        if (size == 0) {
            size = 1;
        }
        for (int i = 0; i < size; i++) {
            aliasRow.getValues().add("");
        }

        int selectedIndex = mainController.getUnionTabPane().getSelectionModel().getSelectedIndex();
        int index = Math.max(selectedIndex, 0);
        boolean exists = false;
        boolean rename = false;
        for (AliasRow item : aliasTable.getItems()) {
            if (item.getAlias().equals(alias)) {
                if (!item.getValues().get(index).isEmpty()) {
                    rename = true;
                    break;
                }
                item.getValues().set(index, name);
                exists = true;
                break;
            }
        }
        if (!exists) {
            aliasRow.getValues().set(index, name);
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

}
