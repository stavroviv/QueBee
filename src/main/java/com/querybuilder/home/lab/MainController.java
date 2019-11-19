package com.querybuilder.home.lab;

import com.sun.javafx.scene.control.skin.LabeledText;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventTarget;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
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

import static com.querybuilder.home.lab.Constants.*;
import static javafx.scene.control.TreeTableView.CONSTRAINED_RESIZE_POLICY;

public class MainController {

    @FXML
    private Button cancelButton;
    @FXML
    private TableView<String> fieldTable;
    private List<SelectItem> selectItems;
    private List<SelectItem> cteList;

    @FXML
    private TabPane qbTabPane_All;
    @FXML
    private TabPane mainTabPane;
    @FXML
    private TableColumn<String, String> fieldColumn;

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

    public MainController(Select sQuery, QueryBuilder queryBuilder) {
        this.queryBuilder = queryBuilder;
        init(sQuery);
    }

    public void init(Select sQuery) {
        this.withItemMap = new HashMap<>();
        this.unionItemMap = new HashMap<>();
        if (this.sQuery != null) {
            this.sQuery = sQuery;
            reloadData();
            return;
        }
        this.sQuery = sQuery;
    }

    public void initialize() {
        initData();
    }

    private void initData() {
        initTables();
        initDatabaseTableView();

        setCellFactories();
        reloadData();
        setPagesHandlers();
    }

    private void setPagesHandlers() {
//        mainTabPane.getSelectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) -> {
//            cteTabPane.setVisible(newTab.getId() == null || !newTab.getId().equals("queryTabPane"));
//        });
        cteTabPane.getSelectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) -> {
            fillCurrentQuery(oldTab);
            showCurrentQuery();
        });
        unionTabPane.getSelectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) -> {
            if (oldTab == null || newTab == null) {
                return;
            }
            showCurrentQuery();
        });
    }

    private void initTables() {
        DBStructure db = new DBStructureImpl();
        databaseTableView.setRoot(db.getDBStructure(this.queryBuilder.getDataSource()));
        dbElements = db.getDbElements();
        joinItems = FXCollections.observableArrayList();
        tablesView.setRoot(new TreeItem<>());
    }

    @FXML
    private TreeTableView<TableRow> databaseTableView;
    @FXML
    private TreeTableColumn<TableRow, TableRow> databaseTableColumn;

    private void initDatabaseTableView() {
        databaseTableView.setOnMousePressed(e -> {
            if (e.getClickCount() == 2 && e.isPrimaryButtonDown()) {
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
                        selectedConditionsTreeTableContext.applyChanges(c);
                        selectedOrderFieldsTree.applyChanges(c);
                    }
                }
        );
        fieldTable.getItems().addListener(
                (ListChangeListener<String>) c -> {
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
    private SelectedFieldsTree selectedConditionsTreeTableContext;
    private SelectedFieldsTree selectedOrderFieldsTree;

    private void initSelectedTables() {
        selectedGroupFieldsTree = new SelectedFieldsTree(tablesView, fieldTable);
        groupFieldsTree.setRoot(selectedGroupFieldsTree);

        selectedConditionsTreeTable = new SelectedFieldsTree(tablesView);
        conditionsTreeTable.setRoot(selectedConditionsTreeTable);

        initConditionTableForPopup();

        selectedOrderFieldsTree = new SelectedFieldsTree(tablesView, fieldTable);
        orderFieldsTree.setRoot(selectedOrderFieldsTree);
    }

    private void initConditionTableForPopup() {
        conditionsTreeTableContext = new TreeTableView<>();
        conditionsTreeTableContext.setShowRoot(false);
        conditionsTreeTableContext.setColumnResizePolicy(CONSTRAINED_RESIZE_POLICY);
        conditionsTreeTableContextColumn = new TreeTableColumn<>();
        conditionsTreeTableContext.getColumns().add(conditionsTreeTableContextColumn);
        selectedConditionsTreeTableContext = new SelectedFieldsTree(tablesView);
        conditionsTreeTableContext.setRoot(selectedConditionsTreeTableContext);
        conditionsTreeTableContext.widthProperty().addListener((ov, t, t1) -> {
            Pane header = (Pane) conditionsTreeTableContext.lookup("TableHeaderRow");
            if (header != null && header.isVisible()) {
                header.setMaxHeight(0);
                header.setMinHeight(0);
                header.setPrefHeight(0);
                header.setVisible(false);
                header.setManaged(false);
            }
        });
        conditionsTreeTableContext.setOnMousePressed(e -> {
            if (e.getClickCount() == 2 && e.isPrimaryButtonDown()) {
                TreeItem<TableRow> item = conditionsTreeTableContext.getSelectionModel().getSelectedItem();
                String parentName = item.getParent().getValue().getName();
                if (DATABASE_ROOT.equals(parentName)) {
                    return;
                }
                ConditionElement conditionElement = conditionTableResults.getSelectionModel().getSelectedItem();
                String name = parentName + "." + item.getValue().getName();
                conditionElement.setLeftExpression(name);
                conditionPopup.hide();
                conditionTableResults.refresh();
            }
        });
    }

    private void setCellFactories() {
        // table columns
        fieldColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()));
        queryCteColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()));
        // cell factory
        setCellFactory(groupFieldsTreeColumn);
        setCellFactory(conditionsTreeTableColumn);
        setCellFactory(conditionsTreeTableContextColumn);
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
        cteTabPane.getTabs().clear();
        queryCteTable.getItems().clear();
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

    private void showCurrentQuery() {
        int cteNumber = cteTabPane.getSelectionModel().getSelectedIndex();
        int unionNumber = 0;

        clearTables();
        Object selectBody;
        if (sQuery.getWithItemsList() == null || cteNumber == sQuery.getWithItemsList().size()) {
            selectBody = sQuery.getSelectBody();
        } else {
            selectBody = sQuery.getWithItemsList().get(cteNumber).getSelectBody();
        }

        if (cteNumberPrev != cteNumber) {
            unionTabPane.getTabs().clear();
        }

        // UNION
        if (selectBody instanceof SetOperationList) {
            SetOperationList setOperationList = (SetOperationList) selectBody;
            if (cteNumberPrev != cteNumber) {
                int i = 1;
                for (SelectBody sBody : setOperationList.getSelects()) {
                    Tab tab = new Tab("Query " + i);
                    tab.setId("Query " + i);
                    unionTabPane.getTabs().add(tab);
                    i++;
                }
            }
            unionNumber = unionTabPane.getSelectionModel().getSelectedIndex();
            SelectBody body = setOperationList.getSelects().get(unionNumber == -1 ? 0 : unionNumber);
            loadSelectData((PlainSelect) body);
        }
        // ONE QUERY
        else if (selectBody instanceof PlainSelect) {
            loadSelectData((PlainSelect) selectBody);
        }
        cteNumberPrev = cteNumber;
    }

    private void loadSelectData(PlainSelect pSelect) {
//        unionTable.getItems().add(new TableRow("Query " + i));
        fillFromTables(pSelect);
        for (Object select : pSelect.getSelectItems()) {
            if (select instanceof SelectExpressionItem) {

                fieldTable.getItems().add(select.toString());
                aliasTable.getItems().add(newAliasItem((SelectExpressionItem) select));

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
                fieldTable.getItems().add(select.toString());
//                    aliasTable.getItems().add(newAliasItem(select));
            }

        }
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
        loadOrderBy(pSelect);
        loadConditions(pSelect);
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
        conditionTableResults.getItems().add(conditionElement);

        Expression leftExpression = where.getLeftExpression();
        while (leftExpression instanceof AndExpression) {
            AndExpression left = (AndExpression) leftExpression;
            ConditionElement condition = new ConditionElement(left.getRightExpression().toString());
            conditionTableResults.getItems().add(condition);
            leftExpression = left.getLeftExpression();
        }
        ConditionElement condition = new ConditionElement(leftExpression.toString());
        conditionTableResults.getItems().add(condition);
    }

    private void loadOrderBy(PlainSelect pSelect) {
        List<OrderByElement> orderByElements = pSelect.getOrderByElements();
        if (orderByElements != null) {
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
                    orderTableResults.getItems().add(0, tableRow);
                }
            });
        }
    }

    private void clearTables() {
        fieldTable.getItems().clear();
        tablesView.getRoot().getChildren().clear();
        joinItems.clear();
        unionTable.getItems().clear();
        aliasTable.getItems().clear();
        conditionTableResults.getItems().clear();
        groupTableResults.getItems().clear();
        groupTableAggregates.getItems().clear();
        orderTableResults.getItems().clear();
    }

    private void fillFromTables(PlainSelect pSelect) {
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
        addFieldRow("test");
    }

    private void addFieldRow(String name) {
        fieldTable.getItems().add(name);
        SelectExpressionItem nSItem = new SelectExpressionItem();
//        nSItem.setAlias(new Alias("test"));
        nSItem.setExpression(new Column(name));

//        List<SelectItem> selectItems = getSelectBody().getSelectItems() == null ? new ArrayList<>() : getSelectBody().getSelectItems();
//        selectItems.add(nSItem);
//        getSelectBody().setSelectItems(selectItems);
    }

    @FXML
    public void deleteFieldRow() {
//        int selectedItem = fieldTable.getSelectionModel().getSelectedIndex();
//        fieldTable.getItems().remove(selectedItem);
//        getSelectBody().getSelectItems().remove(selectedItem);
    }

    @FXML
    public void deleteTableFromSelected() {
        int selectedItem = tablesView.getSelectionModel().getSelectedIndex();
        tablesView.getRoot().getChildren().remove(selectedItem);
    }

    private SelectBody getSelectBody() {
        return getSelectBody(null);
    }

    private SelectBody getSelectBody(Tab tab) {
        SelectBody selectBody;
        if (withItemMap.size() == 0) {
            selectBody = sQuery.getSelectBody();
            if (selectBody == null) {
                initEmptyQuery();
            }
        } else {
            if (tab == null) {
                tab = cteTabPane.getSelectionModel().selectedItemProperty().get();
            }
            selectBody = sQuery.getWithItemsList().get(withItemMap.get(tab.getId())).getSelectBody();
        }
        return selectBody;
    }

    private void initEmptyQuery() {
        PlainSelect plainSelect = new PlainSelect();
//        List<SelectItem> selectItems = new ArrayList<>();
//        plainSelect.setSelectItems(selectItems);
//        FromItem fromItem = new Table();
//        plainSelect.setFromItem(fromItem);
        sQuery.setSelectBody(plainSelect);
    }

    public void addCteTableToForm(Tab tap) {
        qbTabPane_All.getTabs().add(tap);
        queryBatchTable.getItems().add("newwww");
    }

    @FXML
    public void okClick() {
        fillCurrentQuery(null);
        queryBuilder.closeForm(sQuery.toString());
    }

    private void fillCurrentQuery(Tab tab) {
        SelectBody selectBody = getSelectBody(tab);
        try {
            fillOrder(selectBody);
            fillConditions(selectBody);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fillOrder(SelectBody selectBody) {
        List<OrderByElement> orderElements = new ArrayList<>();
        orderTableResults.getItems().forEach(x -> {
            OrderByElement orderByElement = new OrderByElement();
            Column column = new Column(x.getName());
            orderByElement.setExpression(column);
            orderByElement.setAsc(x.getComboBoxValue().equals("Ascending"));
            orderElements.add(orderByElement);

        });
        if (selectBody instanceof PlainSelect) {
            ((PlainSelect) selectBody).setOrderByElements(orderElements);
        }
//
    }

    private void fillConditions(SelectBody selectBody) throws JSQLParserException {
        if (conditionTableResults.getItems().size() == 0) {
            if (selectBody instanceof PlainSelect) {
                ((PlainSelect) selectBody).setWhere(null);
            }
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
        if (selectBody instanceof PlainSelect) {
            ((PlainSelect) selectBody).setWhere(((PlainSelect) select.getSelectBody()).getWhere());
        }
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
            if (e.getClickCount() == 2 && e.isPrimaryButtonDown()) {
                TreeItem<TableRow> selectedItem = tablesView.getSelectionModel().getSelectedItem();
                String parent = selectedItem.getParent().getValue().getName();
                String field = selectedItem.getValue().getName();
                if (!TABLES_ROOT.equals(parent)) {
                    addFieldRow(parent + "." + field);
                }
            }
        });
        setCellFactory(tablesViewColumn);
    }

    private void setCellFactory(TreeTableColumn<TableRow, TableRow> tablesViewColumn) {
        tablesViewColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getValue()));
        tablesViewColumn.setCellFactory(ttc -> new TreeTableCell<TableRow, TableRow>() {
            private final ImageView element = new ImageView(new Image(getClass().getResourceAsStream("/myToolWindow/element.png")));
            private final ImageView table = new ImageView(new Image(getClass().getResourceAsStream("/myToolWindow/table.png")));
            private final ImageView nestedQuery = new ImageView(new Image(getClass().getResourceAsStream("/myToolWindow/nestedQuery.png")));

            @Override
            protected void updateItem(TableRow item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item.getName());
                // icons
                if (empty) {
                    setGraphic(null);
                } else if (item.isNested()) {
                    setGraphic(nestedQuery);
                } else {
                    setGraphic(item.isRoot() ? table : element);
                }
                // context menu
                if ("tablesViewColumn".equals(tablesViewColumn.getId())) {
                    setContextMenu(tableViewGetContextMenu(item, empty));
                }
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
    private TreeTableView<TableRow> conditionsTreeTableContext;
    @FXML
    private TreeTableColumn<TableRow, TableRow> conditionsTreeTableColumn;
    private TreeTableColumn<TableRow, TableRow> conditionsTreeTableContextColumn;

    @FXML
    private TableView<ConditionElement> conditionTableResults;
    @FXML
    private TableColumn<ConditionElement, Boolean> conditionTableResultsCustom;
    @FXML
    private TableColumn<ConditionElement, ConditionElement> conditionTableResultsCondition;

    private void initConditionTableView() {
        conditionPopup = new PopupControl();

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
        conditionTableResultsCondition.setCellFactory(column -> new TableCell<ConditionElement, ConditionElement>() {

            private final ComboBox<String> comparisonComboBox = new ComboBox<>(
                    FXCollections.observableArrayList("=", "<>", "<", ">", "<=", ">=")
            );
            private final TextField customCondition = new TextField();

            @Override
            protected void updateItem(ConditionElement item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else if (item.isCustom()) {
                    if (item.getCondition().isEmpty()) {
                        String cond = item.getLeftExpression() + item.getExpression() + item.getRightExpression();
                        item.setCondition(cond);
                        item.setLeftExpression("");
                        item.setExpression("");
                        item.setRightExpression("");
                    }
                    customCondition.setText(item.getCondition());
                    customCondition.textProperty().addListener(
                            (observable, oldValue, newValue) -> {
                                item.setCondition(newValue);
                            });
                    setGraphic(customCondition);
                } else {
                    HBox pane = new HBox();
                    String condition1 = item.getCondition();
                    if (!condition1.isEmpty()) {
                        String[] array = condition1.split("[>=<=<>]+");
                        String leftExpresion = condition1;
                        String expression = "=";
                        String rightExpression = "?";
                        if (array.length == 2) {
                            leftExpresion = array[0];
                            expression = condition1.replace(array[0], "").replace(array[1], "");
                            comparisonComboBox.setValue(expression);
                            rightExpression = array[1];
                        }
                        item.setLeftExpression(leftExpresion);
                        item.setExpression(expression);
                        item.setRightExpression(rightExpression);
                        item.setCondition("");
                    }

                    Button leftPart = new Button(item.getLeftExpression());
                    leftPart.setMnemonicParsing(false);
                    leftPart.setAlignment(Pos.CENTER_LEFT);
                    leftPart.prefWidthProperty().bind(pane.widthProperty());
                    leftPart.setOnMouseClicked(event -> showPopup(event, this, item));
                    pane.getChildren().add(leftPart);

                    comparisonComboBox.setMinWidth(70);
                    comparisonComboBox.setValue(item.getExpression());
                    comparisonComboBox.valueProperty().addListener(
                            (observable, oldValue, newValue) -> item.setExpression(newValue)
                    );
                    pane.getChildren().add(comparisonComboBox);

                    TextField rightPart = new TextField(item.getRightExpression());
                    rightPart.textProperty().addListener(
                            (observable, oldValue, newValue) -> item.setRightExpression(newValue)
                    );
                    rightPart.prefWidthProperty().bind(pane.widthProperty());
                    pane.getChildren().add(rightPart);

                    setGraphic(pane);
                }
            }
        });
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

    private PopupControl conditionPopup;

    private void showPopup(MouseEvent event, TableCell<ConditionElement, ConditionElement> cell, ConditionElement item) {
        conditionPopup.setAutoHide(true);
        conditionPopup.setAutoFix(true);
        conditionPopup.setHideOnEscape(true);

        EventTarget target = event.getTarget();
        final Button targetButton;
        if (target instanceof Button) {
            targetButton = (Button) target;
        } else {
            LabeledText label = (LabeledText) target;
            targetButton = (Button) label.getParent();
        }
        final Scene scene = targetButton.getScene();
        final Point2D windowCoord = new Point2D(scene.getWindow().getX(), scene.getWindow().getY());
        final Point2D sceneCoord = new Point2D(scene.getX(), scene.getY());
        final Point2D nodeCoord = targetButton.localToScene(0.0, 0.0);
        final double clickX = Math.round(windowCoord.getX() + sceneCoord.getX() + nodeCoord.getX());
        final double clickY = Math.round(windowCoord.getY() + sceneCoord.getY() + nodeCoord.getY());

        conditionPopup.setSkin(new Skin<Skinnable>() {
            @Override
            public Skinnable getSkinnable() {
                return null;
            }

            @Override
            public Node getNode() {
                conditionsTreeTableContext.setMinWidth(targetButton.getWidth());
                conditionsTreeTableContext.setMaxWidth(targetButton.getWidth());
                return conditionsTreeTableContext;
            }

            @Override
            public void dispose() {
            }
        });
        conditionPopup.show(cell, clickX, clickY + targetButton.getHeight());
        conditionTableResults.getSelectionModel().select(item);
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
        QueryBuilder qb = new QueryBuilder(text, false);
        qb.setDataSource(this.queryBuilder.getDataSource());
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
            if (e.getClickCount() == 2 && e.isPrimaryButtonDown()) {
                makeSelect(fieldsTree, resultsTable, defValue);
            }
        });
    }

    private void setResultsTableSelectHandler(TableView<TableRow> groupTableResults, TreeTableView<TableRow> groupFieldsTree) {
        groupTableResults.setOnMousePressed(e -> {
            if (e.getClickCount() == 2 && e.isPrimaryButtonDown()) {
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
        resultsTable.getItems().add(0, tableRow);
        fieldsTree.getRoot().getChildren().remove(selectedItem);
    }

    private void setCellSelectionEnabled(TableView<TableRow> table) {
        table.getSelectionModel().setCellSelectionEnabled(true);
    }

    private void setResultsTablesHandlers() {
        setGroupingHandlers();
        setOrderHandlers();
        setConditionHandlers();
    }

    private void setConditionHandlers() {
        conditionsTreeTable.setOnMousePressed(e -> {
            if (e.getClickCount() == 2 && e.isPrimaryButtonDown()) {
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
                conditionTableResults.getItems().add(0, tableRow);
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
        groupFieldsTree.getRoot().getChildren().add(0, treeItem);
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
    private TableView<TableRow> aliasTable;
    @FXML
    private TableColumn<TableRow, String> aliasFieldColumn;
    @FXML
    private TableColumn<TableRow, String> queryFieldColumn;

    private void initAliasTable() {
        aliasFieldColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAlias()));
        queryFieldColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));

        unionTableNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        unionTableDistinctColumn.setCellFactory(tc -> new CheckBoxTableCell<>());
        unionTableDistinctColumn.setCellValueFactory(data -> new SimpleBooleanProperty(data.getValue().isDistinct()));
    }

    private TableRow newAliasItem(SelectExpressionItem select) {
        Alias alias = select.getAlias();
        String strAlias = alias != null ? select.getAlias().getName() : select.getExpression().toString();
        return new TableRow(select.toString(), strAlias);
    }

    @FXML
    protected void addUnionQuery(ActionEvent event) {
        TableColumn<TableRow, String> newColumn = new TableColumn<>("Query 2");
        aliasTable.getColumns().add(newColumn);
    }


    @FXML
    private TableView<TableRow> unionTable;
    @FXML
    private TableColumn<TableRow, String> unionTableNameColumn;
    @FXML
    private TableColumn<TableRow, Boolean> unionTableDistinctColumn;
}
