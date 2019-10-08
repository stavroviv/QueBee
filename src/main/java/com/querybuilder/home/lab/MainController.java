package com.querybuilder.home.lab;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainController {
    private final static String TABLES_ROOT = "TablesRoot";
    private final static String DATABASE_ROOT = "Tables";

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


    protected QueryBuilder queryBuilder;
    private Map<String, List<String>> dbElements;

    private ObservableList<String> joinItems;

    public MainController(Select sQuery, QueryBuilder queryBuilder) {
        this.queryBuilder = queryBuilder;
        init(sQuery);
    }

    public void init(Select sQuery) {
        if (this.sQuery != null) {
            this.sQuery = sQuery;
            this.withItemMap = new HashMap<>();
            reloadData();
            return;
        }
        this.sQuery = sQuery;
        this.withItemMap = new HashMap<>();
    }

    public void initialize() {
        initData();
    }

    private void initData() {
        initTables();

        mainTabPane.getSelectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) -> {
            cteTabPane.setVisible(newTab.getId() == null || !newTab.getId().equals("queryTabPane"));
        });

        setCellFactories();
        reloadData();
        cteTabPane.getSelectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) -> {
            if (newTab == null) {
                return;
            }
            Integer iii = withItemMap.get(newTab.getId());
            if (iii != null) {
                showCTE(iii);
            }
        });
        initDatabaseTableView();
        initTreeTablesView();
        initLinkTableView();
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

    private SelectedFieldsTree selectedGroupFieldsTree;
    private SelectedFieldsTree selectedConditionsTreeTable;
    private SelectedFieldsTree selectedOrderFieldsTree;
    private SelectedFieldsTree selectedTotalsFieldsTree;

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
                        selectedOrderFieldsTree.applyChanges(c);
                        selectedTotalsFieldsTree.applyChanges(c);
                    }
                }
        );
        fieldTable.getItems().addListener(
                (ListChangeListener<String>) c -> {
                    while (c.next()) {
                        selectedGroupFieldsTree.applyChangesString(c);
                        selectedOrderFieldsTree.applyChangesString(c);
                        selectedTotalsFieldsTree.applyChangesString(c);
                    }
                }
        );
        setResultsTablesHandlers();
        setCellFactory(databaseTableColumn);
        initSelectedTables();
    }

    private void initSelectedTables() {
        selectedGroupFieldsTree = new SelectedFieldsTree(tablesView, fieldTable);
        groupFieldsTree.setRoot(selectedGroupFieldsTree);
        selectedConditionsTreeTable = new SelectedFieldsTree(tablesView);
        conditionsTreeTable.setRoot(selectedConditionsTreeTable);
        selectedOrderFieldsTree = new SelectedFieldsTree(tablesView, fieldTable);
        orderFieldsTree.setRoot(selectedOrderFieldsTree);
        selectedTotalsFieldsTree = new SelectedFieldsTree(tablesView, fieldTable);
        totalsFieldsTree.setRoot(selectedTotalsFieldsTree);
    }

    private void setCellFactories() {
        // table columns
        fieldColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()));
        queryCteColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()));
        // tree columns
        setCellFactory(groupFieldsTreeColumn);
        setCellFactory(conditionsTreeTableColumn);
        setCellFactory(orderFieldsTreeColumn);
        setCellFactory(totalsFieldsTreeColumn);
    }

    private void addTablesRow(String parent) {
        ObservableList<TreeItem<TableRow>> children = tablesView.getRoot().getChildren();
        if (children.stream().noneMatch(x -> x.getValue().getName().equals(parent))) {
            tablesView.getRoot().getChildren().add(getTableItemWithFields(parent));
//            joinItems.add(parent);
            if (getSelectBody().getFromItem() == null) {
                getSelectBody().setFromItem(new Table(parent));
            } else {
                List<Join> jList = (getSelectBody().getJoins() == null) ? new ArrayList<>() : getSelectBody().getJoins();
                Join join = new Join();
                join.setRightItem(new Table(parent));
                join.setSimple(true);
                jList.add(join);
                getSelectBody().setJoins(jList);
            }
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
            showCTE(0);
            return;
        }
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
        showCTE(0);
    }

    private void showCTE(int cteNumber) {
        clearTables();
        Object selectBody;
        if (sQuery.getWithItemsList() == null || cteNumber == sQuery.getWithItemsList().size()) {
            selectBody = sQuery.getSelectBody();
        } else {
            selectBody = sQuery.getWithItemsList().get(cteNumber).getSelectBody();
        }

        if (selectBody instanceof SetOperationList) {
            SetOperationList setOperationList = (SetOperationList) selectBody;
            List selects = setOperationList.getSelects();
            for (Object select : selects) {
                if (select instanceof PlainSelect) {

                } else if (select instanceof SelectExpressionItem) {
                    fieldTable.getItems().add(select.toString());
                }
            }
        } else if (selectBody instanceof PlainSelect) {
            PlainSelect pSelect = (PlainSelect) selectBody;
            fillFromTables(pSelect);
            for (Object select : pSelect.getSelectItems()) {
                fieldTable.getItems().add(select.toString());
            }
        }
    }

    private void clearTables() {
        fieldTable.getItems().clear();
        tablesView.getRoot().getChildren().clear();
        joinItems.clear();
    }

    private void fillFromTables(PlainSelect pSelect) {
        linkTablesPane.setDisable(true);
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

        linkTablesPane.setDisable(false);
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

        List<SelectItem> selectItems = getSelectBody().getSelectItems() == null ? new ArrayList<>() : getSelectBody().getSelectItems();
        selectItems.add(nSItem);
        getSelectBody().setSelectItems(selectItems);
    }

    @FXML
    public void deleteFieldRow() {
        int selectedItem = fieldTable.getSelectionModel().getSelectedIndex();
        fieldTable.getItems().remove(selectedItem);
        getSelectBody().getSelectItems().remove(selectedItem);
    }

    @FXML
    public void deleteTableFromSelected() {
        int selectedItem = tablesView.getSelectionModel().getSelectedIndex();
        tablesView.getRoot().getChildren().remove(selectedItem);
    }

    private PlainSelect getSelectBody() {
        SelectBody selectBody;
        if (withItemMap.size() == 0) {
            selectBody = sQuery.getSelectBody();
            if (selectBody == null) {
                initEmptyQuery();
            }
        } else {
            Tab tab = cteTabPane.getSelectionModel().selectedItemProperty().get();
            selectBody = sQuery.getWithItemsList().get(withItemMap.get(tab.getId())).getSelectBody();
        }
        return (PlainSelect) selectBody;
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
    public void onClickMethod() {
        fillCurrentQuery();
        queryBuilder.closeForm(sQuery.toString());
    }

    private void fillCurrentQuery() {

    }

    @FXML
    public void onDBTableChange() {
//        System.out.println("234");
    }

    @FXML
    public void onCancelClickMethod(ActionEvent actionEvent) {
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
        deleteContext.setOnAction((ActionEvent event) -> {
            deleteTableFromSelected();
        });
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

        linkTableJoinCondition.setCellValueFactory(features -> new ReadOnlyObjectWrapper(features.getValue()));
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
    private TreeTableView<TableRow> groupFieldsTree;
    @FXML
    private TreeTableColumn<TableRow, TableRow> groupFieldsTreeColumn;

    @FXML
    private TreeTableView<TableRow> conditionsTreeTable;
    @FXML
    private TreeTableColumn<TableRow, TableRow> conditionsTreeTableColumn;

    @FXML
    private TreeTableView<TableRow> orderFieldsTree;
    @FXML
    private TreeTableColumn<TableRow, TableRow> orderFieldsTreeColumn;

    @FXML
    private TreeTableView<TableRow> totalsFieldsTree;
    @FXML
    private TreeTableColumn<TableRow, TableRow> totalsFieldsTreeColumn;

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
        qb.setParentController(this);
        qb.setItem(item);
//        qb.setParentController(this);
    }

    public void insertResult(String result, TableRow item, SubSelect subSelect) {
        item.setQuery(result);
        PlainSelect selectBody = getSelectBody();
        if (selectBody.getFromItem().getAlias() != null && selectBody.getFromItem().getAlias().getName().equals(item.getName())) {
            selectBody.setFromItem(subSelect);
        } else {
            selectBody.getJoins().forEach((x) -> {
                if (x.getRightItem().getAlias() != null && x.getRightItem().getAlias().getName().equals(item.getName())) {
                    x.setRightItem(subSelect);
                }
                ;
            });
        }
        System.out.println(selectBody);
    }

    private void setResultsTablesHandlers() {
        setOrderHandlers();
        setTotalsHandlers();
    }

    @FXML
    private TableView<TableRow> orderTableResults;
    @FXML
    private TableColumn<TableRow, String> orderTableResultsFieldColumn;
    @FXML
    private TableColumn<TableRow, String> orderTableResultsSortingColumn;

    private void setOrderHandlers() {
        orderFieldsTree.setOnMousePressed(e -> {
            if (e.getClickCount() == 2 && e.isPrimaryButtonDown()) {
                TreeItem<TableRow> selectedItem = orderFieldsTree.getSelectionModel().getSelectedItem();
                TableRow tableRow = new TableRow(selectedItem.getValue().getName());
                tableRow.setSortingType("Ascending");
                orderTableResults.getItems().add(0, tableRow);
                orderFieldsTree.getRoot().getChildren().remove(selectedItem);
            }
        });

        orderTableResults.getSelectionModel().setCellSelectionEnabled(true);

        orderTableResultsFieldColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        orderTableResultsFieldColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        orderTableResultsFieldColumn.setEditable(false);

        ObservableList<String> items = FXCollections.observableArrayList();
        items.add("Ascending");
        items.add("Descending");
        orderTableResultsSortingColumn.setEditable(true);
        orderTableResultsSortingColumn.setCellFactory(ComboBoxTableCell.forTableColumn(items));
        orderTableResultsSortingColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSortingType()));
    }

    @FXML
    private TableView<TableRow> totalGroupingResults;
    @FXML
    private TableView<TableRow> totalTotalsResults;

    private void setTotalsHandlers() {
    }
}
