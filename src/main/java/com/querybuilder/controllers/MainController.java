package com.querybuilder.controllers;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.RelativePoint;
import com.querybuilder.QueryBuilder;
import com.querybuilder.database.DBStructure;
import com.querybuilder.database.DBStructureImpl;
import com.querybuilder.domain.TableRow;
import com.querybuilder.domain.*;
import com.querybuilder.utils.CustomCell;
import com.querybuilder.utils.Utils;
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
import javafx.scene.layout.AnchorPane;
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

import static com.querybuilder.controllers.SelectedFieldController.FIELD_FORM_CLOSED_EVENT;
import static com.querybuilder.utils.Constants.*;
import static com.querybuilder.utils.Utils.*;

public class MainController implements Argumentative {

    @FXML
    private Button cancelButton;

    @FXML
    private TableView<TableRow> fieldTable;
    @FXML
    private TableColumn<TableRow, String> fieldColumn;

//    private List<SelectItem> selectItems;
//    private List<SelectItem> cteList;

    @FXML
    private TabPane qbTabPane_All;
    @FXML
    private TabPane mainTabPane;
    @FXML
    private TabPane cteTabPane;
    @FXML
    private TabPane unionTabPane;

    private Select sQuery;

    @FXML
    private Spinner<Integer> topSpinner;

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

        initDBTables();
        reloadData();

        setCellFactories();
        setPagesHandlers();

        CustomEventBus.register(this);
    }

    //<editor-fold defaultstate="collapsed" desc="FILL SHOW AUERY">

    private boolean notChangeUnion;

    private void setPagesHandlers() {
//        mainTabPane.getSelectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) -> {
//            cteTabPane.setVisible(newTab.getId() == null || !newTab.getId().equals("queryTabPane"));
//        });
        cteTabPane.getSelectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) -> {
            if (oldTab == null || newTab == null) {
                return;
            }
            saveCurrentQuery(oldTab, null);
            loadCurrentQuery(false);
        });
        unionTabPane.getSelectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) -> {
            if (oldTab == null || newTab == null || notChangeUnion) {
                return;
            }
            saveCurrentQuery(null, oldTab);
            loadCurrentQuery(false);
        });

        setUnionAndCTETabPaneVisibility();
        unionTabPane.getTabs().addListener((ListChangeListener.Change<? extends Tab> change) -> {
            if (change.next()) {
                setUnionAndCTETabPaneVisibility();
            }
        });
        cteTabPane.getTabs().addListener((ListChangeListener.Change<? extends Tab> change) -> {
            if (change.next()) {
                setUnionAndCTETabPaneVisibility();
            }
        });
    }

    private void setUnionAndCTETabPaneVisibility() {
        double anchor = 0.0;
        anchor = setPaneVisibleAnchor(anchor, cteTabPane);
        anchor = setPaneVisibleAnchor(anchor, unionTabPane);
        AnchorPane.setRightAnchor(mainTabPane, anchor);
    }

    private double setPaneVisibleAnchor(double anchor, TabPane pane) {
        if (pane.getTabs().size() <= 1) {
            pane.setVisible(false);
        } else {
            AnchorPane.setRightAnchor(pane, anchor);
            anchor += 29;
            pane.setVisible(true);
        }
        return anchor;
    }

    private void saveCurrentQuery(Tab cteTab, Tab unionTab) {
        PlainSelect newSelectBody = getEmptySelect();
        try {
            saveFromTables(newSelectBody);
            saveSelectedFields(newSelectBody);
            saveOrder(newSelectBody);
            saveConditions(newSelectBody);
        } catch (Exception e) {
            e.printStackTrace();
        }

        int unionNumber;
        if (unionTab == null) {
            int selectedIndex = unionTabPane.getSelectionModel().getSelectedIndex();
            unionNumber = (selectedIndex == -1 ? 0 : selectedIndex);
        } else {
            unionNumber = getTabIndex(unionTab.getId());
        }

        int cteNumber;
        if (cteTab == null) {
            int selectedIndex = cteTabPane.getSelectionModel().getSelectedIndex();
            cteNumber = (selectedIndex == -1 ? 0 : selectedIndex);
        } else {
            cteNumber = getCteTabId(cteTab.getId());
        }

        if (sQuery.getWithItemsList() != null && sQuery.getWithItemsList().size() > 0) {
            SelectBody selectBody;
            if (sQuery.getWithItemsList().size() == cteNumber) {
                selectBody = sQuery.getSelectBody();
            } else {
                selectBody = sQuery.getWithItemsList().get(cteNumber).getSelectBody();
            }
            if (selectBody instanceof SetOperationList) {
                ((SetOperationList) selectBody).getSelects().set(unionNumber, newSelectBody);
            } else {
                if (sQuery.getWithItemsList().size() == cteNumber) {
                    sQuery.setSelectBody(newSelectBody);
                } else {
                    sQuery.getWithItemsList().get(cteNumber).setSelectBody(newSelectBody);
                }
            }
        } else {
            SelectBody selectBody = sQuery.getSelectBody();
            if (selectBody instanceof SetOperationList) {
                ((SetOperationList) selectBody).getSelects().set(unionNumber, newSelectBody);
            } else {
                sQuery.setSelectBody(newSelectBody);
            }
        }
    }

    private int getTabIndex(String unionTabId) {
        int tIndex = 0;
        for (Tab tPane : unionTabPane.getTabs()) {
            if (tPane.getId().equals(unionTabId)) {
                break;
            }
            tIndex++;
        }
        return tIndex;
    }

    private int getCteTabId(String tabId) {
        int tIndex = 0;
        for (Tab tPane : cteTabPane.getTabs()) {
            if (tPane.getId().equals(tabId)) {
                break;
            }
            tIndex++;
        }
        return tIndex;
    }

    private void saveFromTables(PlainSelect selectBody) {
        if (linkTable.getItems().size() == 0) {
            List<Join> jList = new ArrayList<>();
            tablesView.getRoot().getChildren().forEach(x -> {
                String tableName = x.getValue().getName();
                if (selectBody.getFromItem() == null) {
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

    private SelectBody getFullSelectBody() {
        SelectBody selectBody;
        int cteNumber = cteTabPane.getSelectionModel().getSelectedIndex();
        if (sQuery.getWithItemsList() == null || cteNumber == sQuery.getWithItemsList().size()) {
            selectBody = sQuery.getSelectBody();
        } else {
            selectBody = sQuery.getWithItemsList().get(cteNumber).getSelectBody();
        }
        return selectBody;
    }

    private void loadCurrentQuery(boolean firstRun) {
        int cteNumber = cteTabPane.getSelectionModel().getSelectedIndex();
        int unionNumber;
        boolean cteChange = (cteNumberPrev != cteNumber);

        clearTables(cteChange, firstRun);
        SelectBody selectBody = getFullSelectBody();

        if (cteChange || firstRun) {
            unionTabPane.getTabs().clear();
            unionTable.getItems().clear();
            curMaxUnion = 0;
            unionColumns = new HashMap<>();
            // таблица Alias меняется только при переключении CTE
            loadAliasTable(selectBody);
            loadCteTables();
        }

        // UNION
        if (selectBody instanceof SetOperationList) {
            SetOperationList setOperationList = (SetOperationList) selectBody;
            if (cteNumberPrev != cteNumber || firstRun) {
                for (int i = 1; i <= setOperationList.getSelects().size(); i++) {
                    addUnion("Query " + i, i - 1);
                }
                curMaxUnion = setOperationList.getSelects().size() - 1;
            }
            unionNumber = unionTabPane.getSelectionModel().getSelectedIndex();
            SelectBody body = setOperationList.getSelects().get(unionNumber == -1 ? 0 : unionNumber);
            loadSelectData((PlainSelect) body, cteChange);
        }
        // ONE QUERY
        else if (selectBody instanceof PlainSelect) {
            loadSelectData((PlainSelect) selectBody, false);
        } else if (selectBody == null) {
            initSelectedTables();
        }
        cteNumberPrev = cteNumber;
    }

    private void loadCteTables() {
        // загрузить в дерево таблиц предыдущие CTE

    }

    private void loadAliasTable(SelectBody selectBody) {
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
        }
        // ONE QUERY
        else if (selectBody instanceof PlainSelect || selectBody == null) {
            addUnionColumn("Query 1", 0);
            PlainSelect pSelect = (PlainSelect) selectBody;
            if (selectBody != null) {
                addAliasFirstColumn(pSelect);
            }
        }
    }

    static void showMessage(String message) {
        DataContext dataContext = DataManager.getInstance().getDataContext();
        Project project = (Project) dataContext.getData(DataConstants.PROJECT);
        IdeFrame ideFrame = WindowManager.getInstance().getIdeFrame(project);
        FileDocumentManager.getInstance().saveAllDocuments();

        String html = "<html><body>" + message + "</body></html>";
        JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(html, MessageType.INFO, null)
                .setFadeoutTime(10_000)
                .createBalloon()
                .show(RelativePoint.getCenterOf(ideFrame.getStatusBar().getComponent()), Balloon.Position.above);
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

    //</editor-fold>

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
//        initSelectedTables();

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
        selectedOrderFieldsTree = new SelectedFieldsTree(tablesView, orderFieldsTree, fieldTable);
        selectedConditionsTreeTable = new SelectedFieldsTree(tablesView, conditionsTreeTable);
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
        curMaxCTE = 1;

        // one query
        List<WithItem> withItemsList = sQuery.getWithItemsList();
        if (withItemsList == null) {
            loadCurrentQuery(true);
            queryCteTable.getItems().add("Query_1");
            return;
        }

        // CTE
        int i = 0;
        for (WithItem x : withItemsList) {
            String cteName = x.getName();
            Tab tab = new Tab(cteName);
            tab.setId(cteName);
            cteTabPane.getTabs().add(tab);
            queryCteTable.getItems().add(cteName);
            i++;
        }

        curMaxCTE = withItemsList.size() + 1;
        String cteName = "Query_" + (withItemsList.size() + 1);
        addCteTabPane(withItemsList.size() + 1);
        queryCteTable.getItems().add(cteName);

        cteTabPane.getSelectionModel().select(0);
        loadCurrentQuery(true);
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
        loadFromTables(pSelect);
        initSelectedTables();
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

    private void clearIfNotNull(TreeTableView<TableRow> treeTable) {
        if (treeTable.getRoot() != null && treeTable.getRoot().getChildren() != null) {
            treeTable.getRoot().getChildren().clear();
        }
    }

    private void clearTables(boolean cteChange, boolean firstRun) {
        fieldTable.getItems().clear();
        tablesView.getRoot().getChildren().clear();
        joinItems.clear();

        if (!firstRun) {
            clearIfNotNull(conditionsTreeTable);
            clearIfNotNull(groupFieldsTree);
            clearIfNotNull(orderFieldsTree);
        }

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
        if (joins == null || table == null) {
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
//        SelectExpressionItem nSItem = new SelectExpressionItem();
//        nSItem.setAlias(new Alias("test"));
//        nSItem.setExpression(new Column(name));

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

    private PlainSelect getEmptySelect() {
        return new PlainSelect();
    }

    @FXML
    public void okClick() {
        saveCurrentQuery(
                cteTabPane.getSelectionModel().getSelectedItem(),
                unionTabPane.getSelectionModel().getSelectedItem()
        );
        queryBuilder.closeForm(sQuery.toString());
    }

    private void saveSelectedFields(PlainSelect selectBody) {
        List<SelectItem> items = new ArrayList<>();
        fieldTable.getItems().forEach(x -> {
            SelectExpressionItem sItem = new SelectExpressionItem();
            sItem.setExpression(new Column(x.getName()));
            items.add(sItem);
        });
        selectBody.setSelectItems(items);
    }

    private void saveOrder(PlainSelect selectBody) {
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

    private void saveConditions(PlainSelect selectBody) throws JSQLParserException {
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

    //<editor-fold defaultstate="collapsed" desc="TREE SELECTED TABLES VIEW">

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

    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="LINK TABLE">

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
            private final TextField customCondition = new TextField();

            @Override
            protected void updateItem(LinkElement item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else if (item != null && item.isCustom()) {
                    customCondition.setText(item.getCondition());
                    setGraphic(customCondition);
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

    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="INNER QUERY">

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

    //</editor-fold>

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

    private int curMaxUnion; // это индекс максимального объединения, нумерация начинается с 0
    private Map<String, TableColumn> unionColumns;
//    private Map<String, Tab> unionTabs;

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
        SelectBody existingBody = getFullSelectBody();
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

        int cteNumber = cteTabPane.getSelectionModel().getSelectedIndex();
        if (sQuery.getWithItemsList() == null || cteNumber == sQuery.getWithItemsList().size()) {
            sQuery.setSelectBody(selectBody);
        } else {
            sQuery.getWithItemsList().get(cteNumber).setSelectBody(selectBody);
        }
    }

    private void addUnion(String unionName, int curUnion) {
        Tab tab = new Tab(unionName);
        tab.setId(unionName);
        unionTabPane.getTabs().add(tab);
    }

    @FXML
    protected void deleteUnion(ActionEvent event) {
        if (unionTable.getItems().size() == 1) {
            return;
        }

        notChangeUnion = true;
        int selectedIndex = unionTabPane.getSelectionModel().getSelectedIndex();

        TableRow selectedItem = unionTable.getSelectionModel().getSelectedItem();
        String name = selectedItem.getName();
        aliasTable.getColumns().remove(unionColumns.get(name));
        unionTable.getItems().remove(selectedItem);

        int delIndex = getTabIndex(name);
        SelectBody currentSelectBody = sQuery.getSelectBody();
        ((SetOperationList) currentSelectBody).getSelects().remove(delIndex);
        unionTabPane.getTabs().remove(delIndex);
        if (selectedIndex == delIndex) {
            unionTabPane.getSelectionModel().select(delIndex - 1);
            loadCurrentQuery(false);
        }

        notChangeUnion = false;
    }

    @FXML
    private TableView<TableRow> unionTable;
    @FXML
    private TableColumn<TableRow, String> unionTableNameColumn;
    @FXML
    private TableColumn<TableRow, Boolean> unionTableDistinctColumn;

    public void removeCTEClick(ActionEvent actionEvent) {
    }

    private int curMaxCTE; // индекс максимального СTE, нумерация начинается с 0

    @FXML
    private Tab tableAndFieldsTab;

    public void addCTEClick(ActionEvent actionEvent) {
        if (cteTabPane.getTabs().size() == 0) {
            addCteTabPane(1);
        }

        curMaxCTE++;
        String lastQuery = queryCteTable.getItems().get(queryCteTable.getItems().size() - 1);

        Tab newTab = addCteTabPane(curMaxCTE);
        queryCteTable.getItems().add("Query_" + curMaxCTE);

        // добавить новый Select, все что есть превратить в блоки CTE
        List<WithItem> withItemsList = sQuery.getWithItemsList();
        List<WithItem> newItemsList = new ArrayList<>();
        if (withItemsList != null) {
            for (WithItem wIt : sQuery.getWithItemsList()) {
                WithItem wItem = new WithItem();
                wItem.setName(wIt.getName());
                wItem.setSelectBody(wIt.getSelectBody());
                newItemsList.add(wItem);
            }
        }

        addCte(newItemsList, lastQuery);

        sQuery.setWithItemsList(newItemsList);
        sQuery.setSelectBody(getEmptySelect());

        activateNewTab(newTab);
    }

    private void addCte(List<WithItem> newItemsList, String lastQuery) {
        WithItem wItem = new WithItem();
        wItem.setName(lastQuery);
        wItem.setSelectBody(sQuery.getSelectBody());
        newItemsList.add(wItem);
    }

    private void activateNewTab(Tab newTab) {
        SingleSelectionModel<Tab> selectionModel = cteTabPane.getSelectionModel();
        selectionModel.select(newTab);
        SingleSelectionModel<Tab> selModel = mainTabPane.getSelectionModel();
        selModel.select(tableAndFieldsTab);
    }

    private Tab addCteTabPane(int curMaxCTE) {
        String cteName = "Query_" + curMaxCTE;
        Tab tab = new Tab(cteName);
        tab.setId(cteName);
        cteTabPane.getTabs().add(tab);
        return tab;
    }
}