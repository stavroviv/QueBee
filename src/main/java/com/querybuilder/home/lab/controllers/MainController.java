package com.querybuilder.home.lab.controllers;

import com.querybuilder.home.lab.QueryBuilder;
import com.querybuilder.home.lab.database.DBStructure;
import com.querybuilder.home.lab.database.DBStructureImpl;
import com.querybuilder.home.lab.domain.TableRow;
import com.querybuilder.home.lab.domain.*;
import com.querybuilder.home.lab.utils.CustomCell;
import com.querybuilder.home.lab.utils.Utils;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import net.engio.mbassy.listener.Handler;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.querybuilder.home.lab.controllers.SelectedFieldController.FIELD_FORM_CLOSED_EVENT;
import static com.querybuilder.home.lab.utils.Constants.*;
import static com.querybuilder.home.lab.utils.Utils.*;

public class MainController implements Argumentative {

    @FXML
    private Button cancelButton;

    @FXML
    private TableView<TableRow> fieldTable;
    @FXML
    private TableColumn<TableRow, String> fieldColumn;

    private List<SelectItem> selectItems;
    private List<SelectItem> cteList;

    @FXML
    private TabPane qbTabPane_All;
    @FXML
    private TabPane mainTabPane;
    @FXML
    private TabPane cteTabPane;
    @FXML
    private TabPane unionTabPane;

    private Select sQuery;

    public Select getsQuery() {
        return sQuery;
    }

    public void setsQuery(Select sQuery) {
        this.sQuery = sQuery;
    }

    public TableView<String> queryBatchTable;
    @FXML
    private TableView<String> queryCteTable;
    @FXML
    private TableColumn<String, String> queryCteColumn;
    private Map<String, Integer> withItemMap;
    private Map<String, Integer> unionItemMap;

    protected QueryBuilder queryBuilder;
    private Map<String, List<String>> dbElements;

    private ObservableList<String> joinItems;

    @Override
    public void initData(Map<String, Object> userData) {
        this.queryBuilder = (QueryBuilder) userData.get("queryBuilder");
        Select sQuery = (Select) userData.get("sQuery");
        if (this.sQuery != null) {
            this.sQuery = sQuery;
            reloadData();
            return;
        }

        this.sQuery = sQuery;
        this.withItemMap = new HashMap<>();
        this.unionItemMap = new HashMap<>();

        initDBTables();
        setCellFactories();
        reloadData();
        setPagesHandlers();

        CustomEventBus.register(this);
    }

    ////////////////////////////////////////////////////////////////////
    // FILL, SHOW QUERY
    ////////////////////////////////////////////////////////////////////

    private void setPagesHandlers() {
//        mainTabPane.getSelectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) -> {
//            cteTabPane.setVisible(newTab.getId() == null || !newTab.getId().equals("queryTabPane"));
//        });
        cteTabPane.getSelectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) -> {
            fillCurrentQuery(oldTab, null);
            showCurrentQuery();
        });
        unionTabPane.getSelectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) -> {
            if (oldTab == null || newTab == null) {
                return;
            }
            fillCurrentQuery(null, oldTab);
            showCurrentQuery();
        });
    }

    private void fillCurrentQuery(Tab tab, Tab unionTab) {
        PlainSelect selectBody = getSelectBody(tab, unionTab);
        try {
            fillFromTables(selectBody);
            fillSelectedFields(selectBody);
            fillOrder(selectBody);
            fillConditions(selectBody);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fillFromTables(PlainSelect selectBody) {
        if (linkTable.getItems().size() == 0) {
            if (selectBody.getFromItem() != null) {
                selectBody.setFromItem(null);
            }
            List<Join> jList = new ArrayList<>();
            tablesView.getRoot().getChildren().forEach(x -> {
                String tableName = x.getValue().getName();
                if (getSelectBody().getFromItem() == null) {
                    selectBody.setFromItem(new Table(tableName));
                } else {
//                List<Join> jList = (getSelectBody().getJoins() == null) ? new ArrayList<>() : getSelectBody().getJoins();
                    Join join = new Join();
                    join.setRightItem(new Table(tableName));
                    join.setSimple(true);
                    jList.add(join);
                }
            });
            selectBody.setJoins(jList);
            return;
        }


        tablesView.getRoot().getChildren().forEach(x -> {
//            if (getSelectBody().getFromItem() == null) {
//                getSelectBody().setFromItem(new Table(parent));
//            } else {
//                List<Join> jList = (getSelectBody().getJoins() == null) ? new ArrayList<>() : getSelectBody().getJoins();
//                Join join = new Join();
//                join.setRightItem(new Table(parent));
//                join.setSimple(true);
//                jList.add(join);
//                getSelectBody().setJoins(jList);
//            }
        });
    }

    private void showCurrentQuery() {
        int cteNumber = cteTabPane.getSelectionModel().getSelectedIndex();
        int unionNumber;
        boolean cteChange = (cteNumberPrev != cteNumber);

        clearTables(cteChange);

        Object selectBody;
        if (sQuery.getWithItemsList() == null || cteNumber == sQuery.getWithItemsList().size()) {
            selectBody = sQuery.getSelectBody();
        } else {
            selectBody = sQuery.getWithItemsList().get(cteNumber).getSelectBody();
        }

        if (cteChange) {
            unionTabPane.getTabs().clear();
            unionTable.getItems().clear();
            curMaxUnion = 0;
            unionColumns = new HashMap<>();
            unionTabs = new HashMap<>();
            loadAliasTable(selectBody);
        }

        // UNION
        if (selectBody instanceof SetOperationList) {
            SetOperationList setOperationList = (SetOperationList) selectBody;
            if (cteNumberPrev != cteNumber) {
                unionItemMap.clear();
                for (int i = 1; i <= setOperationList.getSelects().size(); i++) {
                    addUnionTabPane("Query " + i);
                    curMaxUnion++;
                }
            }
            unionNumber = unionTabPane.getSelectionModel().getSelectedIndex();
            SelectBody body = setOperationList.getSelects().get(unionNumber == -1 ? 0 : unionNumber);
            loadSelectData((PlainSelect) body, cteChange);
        }
        // ONE QUERY
        else if (selectBody instanceof PlainSelect) {
            loadSelectData((PlainSelect) selectBody, false);
        }
        cteNumberPrev = cteNumber;
    }

    private void loadAliasTable(Object selectBody) {
        aliasTable.getItems().clear();
        int size = aliasTable.getColumns().size();
        aliasTable.getColumns().remove(1, size);
        if (selectBody instanceof SetOperationList) {
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
        }
        // ONE QUERY
        else if (selectBody instanceof PlainSelect) {
            addUnionColumn("Query 1", 0);
            PlainSelect pSelect = (PlainSelect) selectBody;
            addAliasFirstColumn(pSelect);
        }
    }

    private void addAliasFirstColumn(PlainSelect pSelect) {
        for (Object x : pSelect.getSelectItems()) {
            SelectExpressionItem select = (SelectExpressionItem) x;
            Alias alias = select.getAlias();
            String strAlias = alias != null ? select.getAlias().getName() : select.getExpression().toString();
            AliasRow aliasRow = new AliasRow(select.toString(), strAlias);
            aliasRow.getValues().add(strAlias);
            aliasTable.getItems().add(aliasRow);
        }
    }

    private void initDBTables() {
        DBStructure db = new DBStructureImpl();
        databaseTableView.setRoot(db.getDBStructure(this.queryBuilder.getDataSource()));
        dbElements = db.getDbElements();
        joinItems = FXCollections.observableArrayList();
        tablesView.setRoot(new TreeItem<>());
        initDatabaseTableView();
    }

    @FXML
    private TreeTableView<TableRow> databaseTableView;
    @FXML
    private TreeTableColumn<TableRow, TableRow> databaseTableColumn;

    private void initDatabaseTableView() {
        databaseTableView.setOnMousePressed(e -> {
            if (doubleClick(e)) {
                TreeItem<TableRow> selectedItem = databaseTableView.getSelectionModel().getSelectedItem();
                String parent = selectedItem.getParent().getValue().getName();
                String field = selectedItem.getValue().getName();
                if (DATABASE_ROOT.equals(parent)) {
                    addTablesRow(field);
                } else {
                    addTablesRow(parent);
                    addFieldRow(parent + "." + field);
                }
            }
        });
        tablesView.getRoot().getChildren().addListener(
                (ListChangeListener<TreeItem<TableRow>>) c -> {
                    while (c.next()) {
                        selectedGroupFieldsTree.applyChanges(c);
                        selectedConditionsTreeTable.applyChanges(c);
//                        selectedConditionsTreeTableContext.applyChanges(c);
                        selectedOrderFieldsTree.applyChanges(c);
                    }
                }
        );
        fieldTable.getItems().addListener(
                (ListChangeListener<TableRow>) c -> {
                    while (c.next()) {
                        selectedGroupFieldsTree.applyChangesString(c);
                        selectedOrderFieldsTree.applyChangesString(c);
                    }
                }
        );
        setResultsTablesHandlers();
//        setCellFactory(databaseTableColumn);
        initSelectedTables();

        initTreeTablesView();
        initLinkTableView();
        initConditionTableView();
        initAliasTable();
    }


    private SelectedFieldsTree selectedGroupFieldsTree;
    private SelectedFieldsTree selectedConditionsTreeTable;
    private SelectedFieldsTree selectedOrderFieldsTree;

    private void initSelectedTables() {
        selectedGroupFieldsTree = new SelectedFieldsTree(tablesView, groupFieldsTree, fieldTable);
        selectedConditionsTreeTable = new SelectedFieldsTree(tablesView, conditionsTreeTable);
        selectedOrderFieldsTree = new SelectedFieldsTree(tablesView, orderFieldsTree, fieldTable);
    }

    private void setCellFactories() {
        // table columns
        setStringColumnFactory(fieldColumn);
        queryCteColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()));
        // cell factory
        setCellFactory(groupFieldsTreeColumn);
        setCellFactory(conditionsTreeTableColumn);
//        setCellFactory(conditionsTreeTableContextColumn);
        setCellFactory(orderFieldsTreeColumn);
        setCellFactory(databaseTableColumn);
    }

    private void addTablesRow(String parent) {
        ObservableList<TreeItem<TableRow>> children = tablesView.getRoot().getChildren();
        if (children.stream().noneMatch(x -> x.getValue().getName().equals(parent))) {
            tablesView.getRoot().getChildren().add(getTableItemWithFields(parent));
//            joinItems.add(parent);
//            if (getSelectBody().getFromItem() == null) {
//                getSelectBody().setFromItem(new Table(parent));
//            } else {
//                List<Join> jList = (getSelectBody().getJoins() == null) ? new ArrayList<>() : getSelectBody().getJoins();
//                Join join = new Join();
//                join.setRightItem(new Table(parent));
//                join.setSimple(true);
//                jList.add(join);
//                getSelectBody().setJoins(jList);
//            }
        }
    }

    private TreeItem<TableRow> getTableItemWithFields(String tableName) {
        TableRow tableRow1 = new TableRow(tableName);
        tableRow1.setRoot(true);
        TreeItem<TableRow> treeItem = new TreeItem<>(tableRow1);
        List<String> columns = dbElements.get(tableName);
        if (columns != null) {
            columns.forEach(col ->
            {
                TableRow tableRow = new TableRow(col);
                TreeItem<TableRow> tableRowTreeItem = new TreeItem<>(tableRow);
                treeItem.getChildren().add(tableRowTreeItem);
            });
        }
        return treeItem;
    }

    private void reloadData() {
        unionTabPane.getTabs().clear();
        cteTabPane.getTabs().clear();
        queryCteTable.getItems().clear();
        curMaxUnion = 0;

        List<WithItem> withItemsList = sQuery.getWithItemsList();
        if (withItemsList == null) {
            showCurrentQuery();
            return;
        }

        // CTE
        int i = 0;
        for (WithItem x : withItemsList) {
            String cteName = x.getName();
            Tab tab = new Tab(cteName);
            tab.setId(cteName);
            cteTabPane.getTabs().add(tab);
            withItemMap.put(cteName, i);
            i++;
        }

        String cteName = "Query of CTE " + (withItemsList.size() + 1);
        Tab tab = new Tab(cteName);
        tab.setId(cteName);
        cteTabPane.getTabs().add(tab);
        cteTabPane.getSelectionModel().select(0);
        withItemMap.put(cteName, i);
        queryCteTable.getItems().addAll(withItemMap.keySet());
        showCurrentQuery();
    }

    private int cteNumberPrev = -1;

    private void addUnionColumn(String unionName, int i) {
        TableColumn<AliasRow, String> newColumn = new TableColumn<>(unionName);
        newColumn.setEditable(true);

        newColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getValues().get(i)));
        newColumn.setCellFactory(x -> new AliasCell(x, i, getSeletedItems()));

        aliasTable.getSelectionModel().selectedIndexProperty().addListener((num) -> {
            TablePosition focusedCell = aliasTable.getFocusModel().getFocusedCell();
            aliasTable.edit(focusedCell.getRow(), focusedCell.getTableColumn());
        });

        aliasTable.getColumns().add(newColumn);
        unionColumns.put(unionName, newColumn);
        unionTable.getItems().add(new TableRow(unionName));
    }

    private List<String> getSeletedItems() {
        List<String> items = new ArrayList<>();
        fieldTable.getItems().forEach(x -> items.add(x.getName()));
        return items;
    }


    private void loadSelectData(PlainSelect pSelect, boolean cteChange) {
//        unionTable.getItems().add(new TableRow("Query " + i));
        loadFromTables(pSelect);
        loadSelectedFields(pSelect, cteChange);
        loadGroupBy(pSelect);
        loadOrderBy(pSelect);
        loadConditions(pSelect);
    }

    private void loadGroupBy(PlainSelect pSelect) {
        GroupByElement groupBy = pSelect.getGroupBy();
        if (groupBy != null) {
            groupBy.getGroupByExpressions().forEach(x -> {
//                    for (TreeItem<TableRow> ddd: groupFieldsTree.getRoot().getChildren()) {
//                        if (ddd.getValue().getName().equals(x.toString())){
//                            makeSelect(ddd, groupFieldsTree, groupTableResults, null);
//                        }
//                    };
                TableRow tableRow = new TableRow(x.toString());
                groupTableResults.getItems().add(0, tableRow);
            });
        }
    }

    private TableRow newTableRow(String name, int id) {
        TableRow tableRow1 = new TableRow(name);
        tableRow1.setId(id);
        return tableRow1;
    }

    private void loadSelectedFields(PlainSelect pSelect, boolean cteChange) {
        int id = 0;
        for (Object select : pSelect.getSelectItems()) {
            if (select instanceof SelectExpressionItem) {

                fieldTable.getItems().add(newTableRow(select.toString(), id));

                // GROUPING
                SelectExpressionItem select1 = (SelectExpressionItem) select;
                Expression expression1 = select1.getExpression();
                TableRow tableRow;
                if (expression1 instanceof Function) {
                    Function expression = (Function) select1.getExpression();
                    if (expression.getParameters().getExpressions().size() == 1) {
                        String columnName = expression.getParameters().getExpressions().get(0).toString();
                        tableRow = new TableRow(columnName);
                        tableRow.setComboBoxValue(expression.getName());
                        groupTableAggregates.getItems().add(tableRow);
                    }
                }

            } else {
                fieldTable.getItems().add(newTableRow(select.toString(), id));
//                    aliasTable.getItems().add(newAliasItem(select));
            }
            id++;
        }
    }

    private void loadConditions(PlainSelect pSelect) {
        Expression where = pSelect.getWhere();
        if (where == null) {
            return;
        }
        if (where instanceof AndExpression) {
            parseAndExpression((AndExpression) where);
        } else {
            ConditionElement conditionElement = new ConditionElement(where.toString());
            conditionElement.setCustom(true);
            conditionTableResults.getItems().add(conditionElement);
        }
    }

    private void parseAndExpression(AndExpression where) {
        ConditionElement conditionElement = new ConditionElement(where.getRightExpression().toString());
        conditionTableResults.getItems().add(0, conditionElement);

        Expression leftExpression = where.getLeftExpression();
        while (leftExpression instanceof AndExpression) {
            AndExpression left = (AndExpression) leftExpression;
            ConditionElement condition = new ConditionElement(left.getRightExpression().toString());
            conditionTableResults.getItems().add(0, condition);
            leftExpression = left.getLeftExpression();
        }
        ConditionElement condition = new ConditionElement(leftExpression.toString());
        conditionTableResults.getItems().add(0, condition);
    }

    private void loadOrderBy(PlainSelect pSelect) {
        List<OrderByElement> orderByElements = pSelect.getOrderByElements();
        if (orderByElements == null) {
            return;
        }
        orderByElements.forEach(x -> {
            boolean selected = false;
            for (TreeItem<TableRow> ddd : orderFieldsTree.getRoot().getChildren()) {
                if (ddd.getValue().getName().equals(x.getExpression().toString())) {
                    makeSelect(ddd, orderFieldsTree, orderTableResults, x.isAsc() ? "Ascending" : "Descending");
                    selected = true;
                    break;
                }
            }
            if (!selected) {
                TableRow tableRow = new TableRow(x.getExpression().toString());
                tableRow.setComboBoxValue(x.isAsc() ? "Ascending" : "Descending");
                orderTableResults.getItems().add(tableRow);
            }
        });
    }

    private void clearTables(boolean cteChange) {
        fieldTable.getItems().clear();
        tablesView.getRoot().getChildren().clear();
        joinItems.clear();
        conditionTableResults.getItems().clear();
        groupTableResults.getItems().clear();
        groupTableAggregates.getItems().clear();
        orderTableResults.getItems().clear();
        if (cteChange) {
            unionTable.getItems().clear();
//            aliasTable.getItems().clear();
        }
    }

    private void loadFromTables(PlainSelect pSelect) {
//        linkTablesPane.setDisable(true);
        FromItem fromItem = pSelect.getFromItem();
        Table table = null;
        if (fromItem instanceof Table) {
            table = (Table) fromItem;
            tablesView.getRoot().getChildren().add(getTableItemWithFields(table.getName()));
//            conditionsTreeTable.getRoot().getChildren().add(getTableItemWithFields(table.getName()));
        }
        List<Join> joins = pSelect.getJoins();
        if (joins == null) {
            return;
        }

//        linkTablesPane.setDisable(false);
        linkTable.getItems().clear();
        joinItems.add(table.getName());
        for (Join join : joins) {
            FromItem rightItem = join.getRightItem();
            String rightItemName = "";
            if (rightItem instanceof SubSelect) {
                SubSelect sSelect = (SubSelect) rightItem;
                rightItemName = sSelect.getAlias().getName();
                TableRow tableRow = new TableRow(rightItemName);
                tableRow.setNested(true);
                tableRow.setRoot(true);
                String queryText = sSelect.toString().replace(sSelect.getAlias().toString(), "");
                queryText = queryText.substring(1, queryText.length() - 1);
                tableRow.setQuery(queryText);
                TreeItem<TableRow> tableRowTreeItem = new TreeItem<>(tableRow);
                tablesView.getRoot().getChildren().add(tableRowTreeItem);

                PlainSelect plainSelect = (PlainSelect) sSelect.getSelectBody();
                plainSelect.getSelectItems().forEach((sItem) -> {
                    TableRow nestedItem = new TableRow(sItem.toString());
                    TreeItem<TableRow> nestedRow = new TreeItem<>(nestedItem);
                    tableRowTreeItem.getChildren().add(nestedRow);
                });

            } else if (rightItem instanceof Table) {
                rightItemName = rightItem.toString();
                tablesView.getRoot().getChildren().add(getTableItemWithFields(rightItemName));
//                conditionsTreeTable.getRoot().getChildren().add(getTableItemWithFields(rightItem.toString()));
                addLinkElement(table, join);
            }
            joinItems.add(rightItemName);
        }
    }

    private void addLinkElement(Table table, Join join) {
        if (join.getOnExpression() == null) {
            return;
        }
        LinkElement linkElement = new LinkElement(table.getName(), join.getRightItem().toString(), true, false, true, dbElements);
        linkElement.setCondition(join.getOnExpression().toString());
        linkTable.getItems().add(linkElement);
        joinItems.add(join.getRightItem().toString());
    }

    @FXML
    public void addFieldRowAction() {
        Map<String, Object> data = new HashMap<>();
        data.put("selectedFieldsTree", selectedGroupFieldsTree);
        Utils.openForm("/forms/selected-field.fxml", "Custom expression", data);
    }

    @Handler
    public void selectedFieldFormClosedHandler(CustomEvent event) {
        if (!FIELD_FORM_CLOSED_EVENT.equals(event.getName())) {
            return;
        }
        if (event.getCurrentRow() == null) {
            addFieldRow(event.getData());
        } else {
            TableRow tableRow = fieldTable.getItems().get(event.getCurrentRow());
            tableRow.setName(event.getData());
            fieldTable.getItems().set(event.getCurrentRow(), tableRow);
        }
    }

    private void addFieldRow(String name) {
        fieldTable.getItems().add(new TableRow(name));
        SelectExpressionItem nSItem = new SelectExpressionItem();
//        nSItem.setAlias(new Alias("test"));
        nSItem.setExpression(new Column(name));

//        List<SelectItem> selectItems = getSelectBody().getSelectItems() == null ? new ArrayList<>() : getSelectBody().getSelectItems();
//        selectItems.add(nSItem);
//        getSelectBody().setSelectItems(selectItems);
    }

    @FXML
    public void deleteFieldRow() {
        int selectedItem = fieldTable.getSelectionModel().getSelectedIndex();
        fieldTable.getItems().remove(selectedItem);
//        getSelectBody().getSelectItems().remove(selectedItem);
    }

    @FXML
    public void deleteTableFromSelected() {
        int selectedItem = tablesView.getSelectionModel().getSelectedIndex();
        tablesView.getRoot().getChildren().remove(selectedItem);
    }

    private PlainSelect getSelectBody() {
        return getSelectBody(null, null);
    }

    private PlainSelect getSelectBody(Tab tab, Tab unionTab) {
        SelectBody selectBody;
        int unionNumber = 0;
        if (unionTab == null) {
            int selectedIndex = unionTabPane.getSelectionModel().getSelectedIndex();
            unionNumber = (selectedIndex == -1 ? 0 : selectedIndex);
        } else {
            unionNumber = unionItemMap.get(unionTab.getId());
        }

        if (withItemMap.size() == 0) {
            selectBody = sQuery.getSelectBody();
            if (selectBody == null) {
                selectBody = initEmptyQuery();
            }
        } else {
            if (tab == null) {
                tab = cteTabPane.getSelectionModel().selectedItemProperty().get();
            }
            Integer cteNumber = withItemMap.get(tab.getId());
            if (cteNumber.equals(sQuery.getWithItemsList().size())) {
                selectBody = sQuery.getSelectBody();
            } else {
                selectBody = sQuery.getWithItemsList().get(cteNumber).getSelectBody();
            }
        }
        if (selectBody instanceof SetOperationList) {
            selectBody = ((SetOperationList) selectBody).getSelects().get(unionNumber);
        }
        return (PlainSelect) selectBody;
    }

    private PlainSelect initEmptyQuery() {
        PlainSelect plainSelect = new PlainSelect();
        List<SelectItem> selectItems = new ArrayList<>();
        plainSelect.setSelectItems(selectItems);
//        FromItem fromItem = new Table();
//        plainSelect.setFromItem(fromItem);
        sQuery.setSelectBody(plainSelect);
        return plainSelect;
    }

    public void addCteTableToForm(Tab tap) {
        qbTabPane_All.getTabs().add(tap);
        queryBatchTable.getItems().add("newwww");
    }

    @FXML
    public void okClick() {
        fillCurrentQuery(null, null);
        queryBuilder.closeForm(sQuery.toString());
    }

    private void fillSelectedFields(PlainSelect selectBody) {
        if (selectBody.getSelectItems() != null) {
            selectBody.getSelectItems().clear();
        }
        fieldTable.getItems().forEach(x -> {
            SelectExpressionItem sItem = new SelectExpressionItem();
            sItem.setExpression(new Column(x.getName()));
            selectBody.getSelectItems().add(sItem);
        });
    }

    private void fillOrder(PlainSelect selectBody) {
        List<OrderByElement> orderElements = new ArrayList<>();
        orderTableResults.getItems().forEach(x -> {
            OrderByElement orderByElement = new OrderByElement();
            Column column = new Column(x.getName());
            orderByElement.setExpression(column);
            orderByElement.setAsc(x.getComboBoxValue().equals("Ascending"));
            orderElements.add(orderByElement);
        });
        selectBody.setOrderByElements(orderElements);
    }

    private void fillConditions(PlainSelect selectBody) throws JSQLParserException {
        if (conditionTableResults.getItems().size() == 0) {
            selectBody.setWhere(null);
            return;
        }
        StringBuilder where = new StringBuilder();
        for (ConditionElement item : conditionTableResults.getItems()) {
            String whereExpr = item.getCondition();
            if (whereExpr.isEmpty()) {
                whereExpr = item.getLeftExpression() + item.getExpression() + item.getRightExpression();
            }
            where.append(whereExpr).append(" AND ");
        }
        Statement stmt = CCJSqlParserUtil.parse(
                "SELECT * FROM TABLES WHERE " + where.substring(0, where.length() - 4)
        );
        Select select = (Select) stmt;
        Expression whereExpression = ((PlainSelect) select.getSelectBody()).getWhere();
        selectBody.setWhere(whereExpression);
    }

    @FXML
    public void onDBTableChange() {
//        System.out.println("234");
    }

    @FXML
    public void cancelClick(ActionEvent actionEvent) {
        queryBuilder.closeForm();
    }

    /**********************************************
     TREE SELECTED TABLES VIEW
     **********************************************/

    @FXML
    private TreeTableView<TableRow> tablesView;
    @FXML
    private TreeTableColumn<TableRow, TableRow> tablesViewColumn;

    private void initTreeTablesView() {
        tablesView.setOnMousePressed(e -> {
            if (doubleClick(e)) {
                TreeItem<TableRow> selectedItem = tablesView.getSelectionModel().getSelectedItem();
                String parent = selectedItem.getParent().getValue().getName();
                String field = selectedItem.getValue().getName();
                if (!TABLES_ROOT.equals(parent)) {
                    addFieldRow(parent + "." + field);
                }
            }
        });
        setCellFactory(tablesViewColumn);
        tablesViewColumn.setCellFactory(ttc -> new CustomCell() {
            @Override
            protected void updateItem(TableRow item, boolean empty) {
                super.updateItem(item, empty);
                setItem(this, item, empty);
                setContextMenu(tableViewGetContextMenu(item, empty));
            }
        });
    }

    private ContextMenu tableViewGetContextMenu(TableRow item, boolean empty) {
        MenuItem addContext = new MenuItem("Add");
        MenuItem changeContext = new MenuItem("Change");
        MenuItem deleteContext = new MenuItem("Delete");
        MenuItem renameContext = new MenuItem("Rename");

        addContext.setOnAction((ActionEvent event) -> {
//            System.out.println("addContext");
//            Object item = tablesView.getSelectionModel().getSelectedItem();
//            System.out.println("Selected item: " + item);
        });
        deleteContext.setOnAction((ActionEvent event) -> deleteTableFromSelected());
        renameContext.setOnAction((ActionEvent event) -> {
//            System.out.println("renameContext");
//            Object item = tablesView.getSelectionModel().getSelectedItem();
//            System.out.println("Selected item: " + item);
        });
        changeContext.setOnAction((ActionEvent event) -> {
//            System.out.println("changeContext");
//            TableRow item = tablesView.getSelectionModel().getSelectedItem().getValue();
            openNestedQuery(item.getQuery(), item);
        });

        ContextMenu menu = new ContextMenu();
        menu.getItems().add(addContext);
        if (!empty && item.isRoot()) {
            if (item.isNested()) {
                menu.getItems().add(changeContext);
            }
            menu.getItems().add(deleteContext);
            menu.getItems().add(renameContext);
        }
        return menu;
    }

    /**********************************************
     LINK TABLE
     **********************************************/

    @FXML
    private TableView<LinkElement> linkTable;
    @FXML
    private TableColumn<LinkElement, String> linkTableColumnTable1;
    @FXML
    private TableColumn<LinkElement, String> linkTableColumnTable2;
    @FXML
    private TableColumn<LinkElement, Boolean> linkTableAllTable1;
    @FXML
    private TableColumn<LinkElement, Boolean> linkTableAllTable2;
    @FXML
    private TableColumn<LinkElement, Boolean> linkTableCustom;
    @FXML
    private TableColumn<LinkElement, LinkElement> linkTableJoinCondition;
    @FXML
    private Tab linkTablesPane;

    private void initLinkTableView() {
        linkTable.setEditable(true);
        linkTable.getSelectionModel().cellSelectionEnabledProperty().set(true);

        linkTableAllTable1.setCellFactory(tc -> new CheckBoxTableCell<>());
        linkTableAllTable2.setCellFactory(tc -> new CheckBoxTableCell<>());

        linkTableCustom.setCellFactory(column -> new CheckBoxTableCell<>());
        linkTableCustom.setCellValueFactory(cellData -> {
            LinkElement cellValue = cellData.getValue();
            BooleanProperty property = cellValue.customProperty();
            property.addListener((observable, oldValue, newValue) -> {
                cellValue.setCustom(newValue);
                linkTable.refresh();
            });
            return property;
        });

        ObservableList<String> joinItems1 = FXCollections.observableArrayList();
        joinItems1.addAll(joinItems);
        linkTableColumnTable1.setCellFactory(ComboBoxTableCell.forTableColumn(joinItems1));

        ObservableList<String> joinItems2 = FXCollections.observableArrayList();
        joinItems2.addAll(joinItems);
        linkTableColumnTable2.setCellFactory(ComboBoxTableCell.forTableColumn(joinItems2));

        linkTableJoinCondition.setCellValueFactory(features -> new ReadOnlyObjectWrapper<>(features.getValue()));
        linkTableJoinCondition.setCellFactory(column -> new TableCell<LinkElement, LinkElement>() {
            private final ObservableList<String> comparison = FXCollections.observableArrayList("=", "<>", "<", ">", "<=", ">=");
            private final ComboBox<String> comparisonComboBox = new ComboBox<>(comparison);
            private final TextField customConditon = new TextField();

            @Override
            protected void updateItem(LinkElement item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else if (item != null && item.isCustom()) {
                    customConditon.setText(item.getCondition());
                    setGraphic(customConditon);
                } else {
                    HBox pane = new HBox();
                    item.getConditionComboBox1().prefWidthProperty().bind(pane.widthProperty());
                    comparisonComboBox.setMinWidth(70);
                    item.getConditionComboBox2().prefWidthProperty().bind(pane.widthProperty());

                    String condition = item.getCondition();
                    if (condition != null) {
                        String[] array = condition.split("[>=<=<>]");
                        item.getConditionComboBox1().setValue(array[0]);
                        comparisonComboBox.setValue(condition.replace(array[0], "").replace(array[1], ""));
                        item.getConditionComboBox2().setValue(array[1]);
                    }
                    pane.getChildren().add(item.getConditionComboBox1());
                    pane.getChildren().add(comparisonComboBox);
                    pane.getChildren().add(item.getConditionComboBox2());
                    setGraphic(pane);
                }

            }
        });
    }

    @FXML
    protected void addLinkElement(ActionEvent event) {
        linkTable.getItems().add(new LinkElement("", "", false, false, false, dbElements));
    }

    @FXML
    protected void copyLinkElement(ActionEvent event) {
        LinkElement selectedItem = linkTable.getSelectionModel().getSelectedItem();
        linkTable.getItems().add(selectedItem.clone());
    }

    @FXML
    protected void deleteLinkElement(ActionEvent event) {
        LinkElement selectedItem = linkTable.getSelectionModel().getSelectedItem();
        linkTable.getItems().remove(selectedItem);
    }

    @FXML
    private TreeTableView<TableRow> conditionsTreeTable;
    @FXML
    private TreeTableColumn<TableRow, TableRow> conditionsTreeTableColumn;
    @FXML
    private TableView<ConditionElement> conditionTableResults;
    @FXML
    private TableColumn<ConditionElement, Boolean> conditionTableResultsCustom;
    @FXML
    private TableColumn<ConditionElement, ConditionElement> conditionTableResultsCondition;

    private void initConditionTableView() {
        conditionTableResults.setEditable(true);
        conditionTableResults.getSelectionModel().cellSelectionEnabledProperty().set(true);

        conditionTableResultsCustom.setCellFactory(column -> new CheckBoxTableCell<>());
        conditionTableResultsCustom.setCellValueFactory(cellData -> {
            ConditionElement cellValue = cellData.getValue();
            BooleanProperty property = cellValue.customProperty();
            property.addListener((observable, oldValue, newValue) -> {
                cellValue.setCustom(newValue);
                conditionTableResults.refresh();
            });
            return property;
        });

        conditionTableResultsCondition.setCellValueFactory(column -> new ReadOnlyObjectWrapper<>(column.getValue()));
        conditionTableResultsCondition.setCellFactory(column -> new ConditionCell(conditionTableResults, tablesView));
    }

    @FXML
    public void addCondition() {
        conditionTableResults.getItems().add(new ConditionElement(""));
    }

    @FXML
    public void deleteCondition() {
        ConditionElement selectedItem = conditionTableResults.getSelectionModel().getSelectedItem();
        conditionTableResults.getItems().remove(selectedItem);
    }

    @FXML
    public void copyCondition() {
        ConditionElement selectedItem = conditionTableResults.getSelectionModel().getSelectedItem();
        ConditionElement conditionElement = new ConditionElement("");
        conditionElement.setName(selectedItem.getName());
        conditionTableResults.getItems().add(conditionElement);
    }

    /*
    INNER QUERY
     */
    @FXML
    private Button addInnerQuery;

    @FXML
    public void addInnerQueryOnClick() {
        openNestedQuery("", null);
    }

    private void openNestedQuery(String text, TableRow item) {
        QueryBuilder qb = new QueryBuilder(text, false, this.queryBuilder.getDataSource());
//        qb.setDataSource();,
        qb.setParentController(this);
        qb.setItem(item);
//        qb.setParentController(this);
    }

    public void insertResult(String result, TableRow item, SubSelect subSelect) {
//        item.setQuery(result);
//        PlainSelect selectBody = getSelectBody();
//        if (selectBody.getFromItem().getAlias() != null && selectBody.getFromItem().getAlias().getName().equals(item.getName())) {
//            selectBody.setFromItem(subSelect);
//        } else {
//            selectBody.getJoins().forEach((x) -> {
//                if (x.getRightItem().getAlias() != null && x.getRightItem().getAlias().getName().equals(item.getName())) {
//                    x.setRightItem(subSelect);
//                }
//            });
//        }
//        System.out.println(selectBody);
    }


    private void setStringColumnFactory(TableColumn<TableRow, String> resultsColumn) {
        setStringColumnFactory(resultsColumn, false);
    }

    private void setStringColumnFactory(TableColumn<TableRow, String> resultsColumn, boolean editable) {
        resultsColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        resultsColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        resultsColumn.setEditable(editable);
    }

    private void setTreeSelectHandler(TreeTableView<TableRow> fieldsTree, TableView<TableRow> resultsTable) {
        setTreeSelectHandler(fieldsTree, resultsTable, "");
    }

    private void setTreeSelectHandler(TreeTableView<TableRow> fieldsTree,
                                      TableView<TableRow> resultsTable,
                                      String defValue) {
        fieldsTree.setOnMousePressed(e -> {
            if (doubleClick(e)) {
                makeSelect(fieldsTree, resultsTable, defValue);
            }
        });
    }

    private void setResultsTableSelectHandler(TableView<TableRow> groupTableResults, TreeTableView<TableRow> groupFieldsTree) {
        groupTableResults.setOnMousePressed(e -> {
            if (doubleClick(e)) {
                makeDeselect(groupTableResults, groupFieldsTree);
            }
        });
    }

    private void makeSelect(TreeTableView<TableRow> fieldsTree, TableView<TableRow> resultsTable) {
        makeSelect(fieldsTree, resultsTable, null);
    }

    private void makeSelect(TreeTableView<TableRow> fieldsTree,
                            TableView<TableRow> resultsTable, String defaultValue) {
        makeSelect(null, fieldsTree, resultsTable, defaultValue);
    }

    private void makeSelect(TreeItem<TableRow> selectedItem, TreeTableView<TableRow> fieldsTree,
                            TableView<TableRow> resultsTable, String defaultValue) {
        if (selectedItem == null) {
            selectedItem = fieldsTree.getSelectionModel().getSelectedItem();
        }

        if (selectedItem.getChildren().size() > 0) {
            return;
        }
        String name = selectedItem.getValue().getName();
        TreeItem<TableRow> parent = selectedItem.getParent();
        if (parent != null) {
            String parentName = parent.getValue().getName();
            if (!parentName.equals(DATABASE_ROOT)) {
                name = parentName + "." + name;
            }
        }
        TableRow tableRow = new TableRow(name);
        if (defaultValue != null) {
            tableRow.setComboBoxValue(defaultValue);
        }
        resultsTable.getItems().add(tableRow);
        fieldsTree.getRoot().getChildren().remove(selectedItem);
    }

    private void setCellSelectionEnabled(TableView<TableRow> table) {
        table.getSelectionModel().setCellSelectionEnabled(true);
    }

    private void setResultsTablesHandlers() {
        setGroupingHandlers();
        setOrderHandlers();
        setConditionHandlers();
        setSelectedFieldsHandlers();
    }

    @FXML
    private void editFieldClick() {
        editField();
    }

    private void setSelectedFieldsHandlers() {
        fieldTable.setOnMousePressed(e -> {
            if (doubleClick(e)) {
                editField();
            }
        });
    }

    private void editField() {
        TableRow selectedItem = fieldTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("selectedFieldsTree", selectedGroupFieldsTree);
        data.put("selectedItem", selectedItem);
        data.put("currentRow", fieldTable.getSelectionModel().getSelectedIndex());
        Utils.openForm("/forms/selected-field.fxml", "Custom expression", data);
    }

    private void setConditionHandlers() {
        conditionsTreeTable.setOnMousePressed(e -> {
            if (doubleClick(e)) {
                TreeItem<TableRow> selectedItem = conditionsTreeTable.getSelectionModel().getSelectedItem();
                if (selectedItem == null) {
                    return;
                }
                if (selectedItem.getChildren().size() > 0) {
                    return;
                }
                String name = selectedItem.getValue().getName();
                TreeItem<TableRow> parent = selectedItem.getParent();
                if (parent != null) {
                    String parentName = parent.getValue().getName();
                    if (!parentName.equals(DATABASE_ROOT)) {
                        name = parentName + "." + name;
                    }
                }
                ConditionElement tableRow = new ConditionElement(name);
                conditionTableResults.getItems().add(tableRow);
                conditionsTreeTable.getRoot().getChildren().remove(selectedItem);
            }
        });
    }

    @FXML
    protected void selectOrder(ActionEvent event) {
        makeSelect(orderFieldsTree, orderTableResults, ORDER_DEFAULT_VALUE);
    }

    @FXML
    protected void deselectOrder(ActionEvent event) {
        makeDeselect(orderTableResults, orderFieldsTree);
    }

    private void makeDeselect(TableView<TableRow> groupTableResults, TreeTableView<TableRow> groupFieldsTree) {
        TableRow selectedItem = groupTableResults.getSelectionModel().getSelectedItem();
        if (groupTableResults.getId().equals("orderTableResults")
                && groupTableResults.getSelectionModel().getSelectedCells().get(0).getColumn() == 1) {
            return;
        }
        TableRow tableRow = new TableRow(selectedItem.getName());
        if (tableRow.isNotSelectable()) {
            return;
        }
        TreeItem<TableRow> treeItem = new TreeItem<>(tableRow);
        addElement(groupFieldsTree.getRoot().getChildren(), treeItem);
        groupTableResults.getItems().remove(selectedItem);
    }

    private void setComboBoxColumnFactory(TableColumn<TableRow, String> column, String... items) {
        column.setEditable(true);
        column.setCellFactory(
                ComboBoxTableCell.forTableColumn(FXCollections.observableArrayList(items))
        );
        column.setCellValueFactory(cellData -> cellData.getValue().comboBoxValueProperty());
    }

    @FXML
    private TreeTableView<TableRow> groupFieldsTree;
    @FXML
    private TreeTableColumn<TableRow, TableRow> groupFieldsTreeColumn;

    @FXML
    private TableView<TableRow> groupTableResults;
    @FXML
    private TableColumn<TableRow, String> groupTableResultsFieldColumn;

    @FXML
    private TableView<TableRow> groupTableAggregates;
    @FXML
    private TableColumn<TableRow, String> groupTableAggregatesFieldColumn;
    @FXML
    private TableColumn<TableRow, String> groupTableAggregatesFunctionColumn;

    private void setGroupingHandlers() {
        setStringColumnFactory(groupTableResultsFieldColumn);
        setStringColumnFactory(groupTableAggregatesFieldColumn);

        setCellSelectionEnabled(groupTableAggregates);
        setComboBoxColumnFactory(groupTableAggregatesFunctionColumn,
                GROUP_DEFAULT_VALUE, "AVG", "COUNT", "MIN", "MAX");

        setTreeSelectHandler(groupFieldsTree, groupTableResults);
        setResultsTableSelectHandler(groupTableResults, groupFieldsTree);
    }

    @FXML
    protected void selectGroup(ActionEvent event) {
        makeSelect(groupFieldsTree, groupTableResults);
    }

    @FXML
    protected void deselectGroup(ActionEvent event) {
        makeDeselect(groupTableResults, groupFieldsTree);
    }

    @FXML
    protected void selectAggregate(ActionEvent event) {
        makeSelect(groupFieldsTree, groupTableAggregates, GROUP_DEFAULT_VALUE);
    }

    @FXML
    protected void deselectAggregate(ActionEvent event) {
        makeDeselect(groupTableAggregates, groupFieldsTree);
    }

    @FXML
    private Button orderUpButton;
    @FXML
    private Button orderDownButton;

    @FXML
    protected void orderUp(ActionEvent event) {
        int index = orderTableResults.getSelectionModel().getSelectedIndex();
        orderTableResults.getItems().add(index - 1, orderTableResults.getItems().remove(index));
        orderTableResults.getSelectionModel().clearAndSelect(index - 1);
    }

    @FXML
    protected void orderDown(ActionEvent event) {
        int index = orderTableResults.getSelectionModel().getSelectedIndex();
        orderTableResults.getItems().add(index + 1, orderTableResults.getItems().remove(index));
        orderTableResults.getSelectionModel().clearAndSelect(index + 1);
    }

    @FXML
    private TreeTableView<TableRow> orderFieldsTree;
    @FXML
    private TreeTableColumn<TableRow, TableRow> orderFieldsTreeColumn;

    @FXML
    private TableView<TableRow> orderTableResults;
    @FXML
    private TableColumn<TableRow, String> orderTableResultsFieldColumn;
    @FXML
    private TableColumn<TableRow, String> orderTableResultsSortingColumn;

    private void setOrderHandlers() {
//        setCellSelectionEnabled(orderTableResults);
        setTreeSelectHandler(orderFieldsTree, orderTableResults, ORDER_DEFAULT_VALUE);
        setStringColumnFactory(orderTableResultsFieldColumn);

        setComboBoxColumnFactory(orderTableResultsSortingColumn, ORDER_DEFAULT_VALUE, "Descending");
        setResultsTableSelectHandler(orderTableResults, orderFieldsTree);

        // buttons
        ReadOnlyIntegerProperty selectedIndex = orderTableResults.getSelectionModel().selectedIndexProperty();
        orderUpButton.disableProperty().bind(selectedIndex.lessThanOrEqualTo(0));
        orderDownButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            int index = selectedIndex.get();
            return index < 0 || index + 1 >= orderTableResults.getItems().size();
        }, selectedIndex, orderTableResults.getItems()));
    }

    @FXML
    private TableView<AliasRow> aliasTable;
    @FXML
    private TableColumn<AliasRow, String> aliasFieldColumn;

    private void initAliasTable() {
        aliasTable.getSelectionModel().cellSelectionEnabledProperty().set(true);

        aliasFieldColumn.setCellValueFactory(new PropertyValueFactory<>("alias"));
        aliasFieldColumn.setCellFactory(TextFieldTableCell.forTableColumn());
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

    private int curMaxUnion;
    private Map<String, TableColumn> unionColumns;
    private Map<String, Tab> unionTabs;

    @FXML
    protected void addUnionQuery(ActionEvent event) {
        curMaxUnion++;
        String unionName = "Query " + curMaxUnion;
        aliasTable.getItems().forEach(x -> x.getValues().add(""));
        addUnionColumn(unionName, curMaxUnion - 1);
        addUnionTabPane(unionName);
    }

    private void addUnionTabPane(String unionName) {
        Tab tab = new Tab(unionName);
        tab.setId(unionName);
        unionTabPane.getTabs().add(tab);
        unionTabs.put(unionName, tab);
        unionItemMap.put(unionName, curMaxUnion);
    }

    @FXML
    protected void deleteUnion(ActionEvent event) {
        if (unionTable.getItems().size() == 1) {
            return;
        }
        TableRow selectedItem = unionTable.getSelectionModel().getSelectedItem();
        aliasTable.getColumns().remove(unionColumns.get(selectedItem.getName()));
        unionTabPane.getTabs().remove(unionTabs.get(selectedItem.getName()));
        unionTable.getItems().remove(selectedItem);
        unionItemMap.remove(selectedItem.getName());
    }

    @FXML
    private TableView<TableRow> unionTable;
    @FXML
    private TableColumn<TableRow, String> unionTableNameColumn;
    @FXML
    private TableColumn<TableRow, Boolean> unionTableDistinctColumn;

}
