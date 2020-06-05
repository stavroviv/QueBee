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
import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.parser.ParseException;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.querybuilder.utils.Constants.EMPTY_UNION_VALUE;
import static com.querybuilder.utils.Utils.*;

@Data
@EqualsAndHashCode(callSuper = false)
public class UnionAliases extends AbstractQueryPart {
    private static final String FIRST_COLUMN = "Query 1";
    private static final String SECOND_COLUMN = "Query 2";

    private int curMaxUnion; // индекс максимального объединения, нумерация начинается с 1
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

    public void loadUnionTabPanes(SetOperationList setOperationList) {
        for (int i = 1; i <= setOperationList.getSelects().size(); i++) {
            addUnionTabPane("Query " + i);
        }
        curMaxUnion = setOperationList.getSelects().size();
    }

    @Override
    public void save(PlainSelect pSelect) {
        // custom save
    }

    public void saveAliases() {
        Select sQuery = mainController.getSQuery();
        SelectBody selectBody = sQuery.getSelectBody();
        if (selectBody instanceof SetOperationList) {

            int index = 0;
            for (String union : unionColumns.keySet()) {
                List<SelectItem> items = new ArrayList<>();
                for (AliasRow item : aliasTable.getItems()) {

                    String name = item.getValues().get(union);//get first ket from unionColumns
                    if (name == null) {
                        continue;
                    }
                    Expression expression = null;
                    try {
                        expression = new CCJSqlParser(name).Expression();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    SelectExpressionItem sItem = new SelectExpressionItem();
                    if (index == 0 && !name.equals(item.getAlias())) {
                        Alias alias = new Alias(item.getAlias(), true);
                        sItem.setAlias(alias);
                    }
                    sItem.setExpression(expression);
                    items.add(sItem);
                }
                if (!items.isEmpty()) {
                    List<SelectBody> selects = ((SetOperationList) selectBody).getSelects();
                    PlainSelect sb = (PlainSelect) selects.get(index);
                    sb.setSelectItems(items);
                }

                index++;
                //   selects.set(unionNumber, newSelectBody);
//                ((PlainSelect) selectBody).setSelectItems(items);
            }
        } else {
            List<SelectItem> items = new ArrayList<>();
            for (AliasRow item : aliasTable.getItems()) {
                String name = item.getValues().get(FIRST_COLUMN);//get first ket from unionColumns
                Expression expression = null;
                try {
                    expression = new CCJSqlParser(name).Expression();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                SelectExpressionItem sItem = new SelectExpressionItem();
                if (!name.equals(item.getAlias())) {
                    Alias alias = new Alias(item.getAlias(), true);
                    sItem.setAlias(alias);
                }
                sItem.setExpression(expression);
                items.add(sItem);
            }
            ((PlainSelect) selectBody).setSelectItems(items);

        }
//        List<SelectItem> selectItems = pSelect.getSelectItems();
//        int i = 0;
//        for (SelectItem selectItem : selectItems) {
//            if (selectItem instanceof SelectExpressionItem) {
//                Expression expression = ((SelectExpressionItem) selectItem).getExpression();
//                if (expression instanceof Column) {
//                    Column column = (Column) expression;
//                    AliasRow aliasRow = aliasTable.getItems().get(i);
//                    if (!column.getColumnName().equals(aliasRow.getAlias())) {
//                        Alias alias = new Alias(aliasRow.getAlias(), true);
//                        ((SelectExpressionItem) selectItem).setAlias(alias);
//                    }
//                }
//            }
//            i++;
//        }
//        pSelect.setSelectItems(selectItems);
    }

    public void loadAliases(SelectBody selectBody) {
        unionTable.getItems().clear();
        unionColumns = new HashMap<>();
        aliasTable.getItems().clear();
        curMaxUnion = 1;

        int size = aliasTable.getColumns().size();
        aliasTable.getColumns().remove(1, size);
        if (selectBody instanceof SetOperationList) { // UNION
            SetOperationList setOperationList = (SetOperationList) selectBody;
            curMaxUnion = setOperationList.getSelects().size();
            int i = 1;
            for (SelectBody sBody : setOperationList.getSelects()) {
                String unionName = "Query " + i;
                addUnionColumn(unionName);
                PlainSelect pSelect = (PlainSelect) sBody;
                if (i == 1) {
                    addAliasFirstColumn(pSelect);
                } else {
                    int j = 0;
                    if (pSelect.getSelectItems().size() > aliasTable.getItems().size()) {
                        throw new RuntimeException("Different number of fields in union queries");
                    }
                    for (SelectItem x : pSelect.getSelectItems()) {
                        SelectExpressionItem select = (SelectExpressionItem) x;
                        Alias alias = select.getAlias();
                        String strAlias = alias != null ? select.getAlias().getName() : select.getExpression().toString();
                        AliasRow aliasEl = aliasTable.getItems().get(j);
                        aliasEl.getValues().put(unionName, strAlias);
                        j++;
                    }
                }
                i++;
            }
        } else if (selectBody instanceof PlainSelect || selectBody == null) { // ONE QUERY
            addUnionColumn(FIRST_COLUMN);
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
            aliasRow.getValues().put(FIRST_COLUMN, getNameFromColumn((Column) expression));
            aliasTable.getItems().add(aliasRow);
        }

        setAliasesIds();
    }

    public void setAliasesIds() {
        if (aliasTable.getItems().isEmpty()) {
            return;
        }
        ObservableList<TableRow> items = mainController.getTableFieldsController().getFieldTable().getItems();
        int i = 0;
        for (TableRow item : items) {
            aliasTable.getItems().get(i).setId(item.getId());
            i++;
        }
    }

    public void addUnionColumn(String unionName) {
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

        newColumn.setCellFactory(x -> new AliasCell(x, unionName, mainController));

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
        addNewUnion();

        if (mainController.getUnionTabPane().getTabs().isEmpty()) {
            addFirstUnion();
            return;
        }

        curMaxUnion++;
        String unionName = "Query " + curMaxUnion;
        addUnionColumn(unionName);
        Tab tab = addUnionTabPane(unionName);
        activateNewTab(tab, mainController.getUnionTabPane(), mainController);
    }

    private void addFirstUnion() {
        addUnionTabPane(FIRST_COLUMN);
        Tab tab = addUnionTabPane(SECOND_COLUMN);
        addUnionColumn(SECOND_COLUMN);
        activateNewTab(tab, mainController.getUnionTabPane(), mainController);
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

    public Tab addUnionTabPane(String unionName) {
        Tab tab = new Tab(unionName);
        tab.setId(unionName);
        mainController.getUnionTabPane().getTabs().add(tab);
        return tab;
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
        unionColumns.remove(name);
        for (AliasRow item : aliasTable.getItems()) {
            item.getValues().remove(name);
        }

        int delIndex = getTabIndex(mainController, name);
        SelectBody currentSelectBody = mainController.getSQuery().getSelectBody();
        ((SetOperationList) currentSelectBody).getSelects().remove(delIndex);
        unionTabPane.getTabs().remove(delIndex);
        if (selectedIndex == delIndex) {
            unionTabPane.getSelectionModel().select(delIndex - 1);
            mainController.loadCurrentQuery(false);
        }

        mainController.setNotChangeUnion(false);
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
            if (item.getValues().get(curUnion) != null) {
                rename = true;
                break;
            }
            item.getValues().put(curUnion, name);
            exists = true;
            break;
        }
        if (!exists) {
            for (String column : unionColumns.keySet()) {
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

}
