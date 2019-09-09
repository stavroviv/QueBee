package com.querybuilder.home.lab;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainController {
    private final static String TABLES_ROOT = "TablesRoot";
    private final static String DATABASE_ROOT = "Tables";

    @FXML
    private Button okButton;
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

    private ObservableList<String> items;

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
        DBStructure db = new DBStructureImpl();
        databaseTableView.setRoot(db.getDBStructure());
        dbElements = db.getDbElements();
        items = FXCollections.observableArrayList();
        tablesView.setRoot(new TreeItem<>());
        groupFieldsTree.setRoot(new TreeItem<>());
        conditionsTreeTable.setRoot(new TreeItem<>());

        mainTabPane.getSelectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) -> {
            cteTabPane.setVisible(newTab.getId() == null || !newTab.getId().equals("queryTabPane"));
        });

        setCellFactories();
//
//        databaseTableView.setOnMousePressed(e -> {
//            if (e.getClickCount() == 2 && e.isPrimaryButtonDown()) {
//                TreeItem<String> selectedItem = databaseTableView.getSelectionModel().getSelectedItem();
//                String parent = selectedItem.getParent().getValue();
//                String field = selectedItem.getValue();
//                if (DATABASE_ROOT.equals(parent)) {
//                    addTablesRow(field);
//                } else {
//                    addTablesRow(parent);
//                    addFieldRow(parent + "." + field);
//                }
//            }
//        });

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
        setCellFactory(databaseTableColumn);
    }

    private void setCellFactories() {
        // table columns
        fieldColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()));
        queryCteColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()));
        // tree columns
//        databaseTableColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getValue()));
        groupFieldsTreeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getValue()));
        conditionsTreeTableColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getValue()));
    }

    private void addTablesRow(String parent) {
        ObservableList<TreeItem<TableRow>> children = tablesView.getRoot().getChildren();
        if (children.stream().noneMatch(x -> x.getValue().getName().equals(parent))) {
            tablesView.getRoot().getChildren().add(getTableItemWithFields(parent));
//            conditionsTreeTable.getRoot().getChildren().add(getTableItemWithFields(parent));
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
//                        addRowToGroup(tableName + "." + col);
                    }
            );
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
        items.clear();
        groupFieldsTree.getRoot().getChildren().clear();
        conditionsTreeTable.getRoot().getChildren().clear();
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
        items.add(table.getName());
        for (Join join : joins) {
            FromItem rightItem = join.getRightItem();
            if (rightItem instanceof SubSelect) {
                SubSelect sSelect = (SubSelect) rightItem;
                TableRow tableRow = new TableRow(sSelect.getAlias().getName());
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
                tablesView.getRoot().getChildren().add(getTableItemWithFields(rightItem.toString()));
//                conditionsTreeTable.getRoot().getChildren().add(getTableItemWithFields(rightItem.toString()));
                addLinkElement(table, join);
            }
        }
    }

    private void addLinkElement(Table table, Join join) {
        LinkElement linkElement = new LinkElement(table.getName(), join.getRightItem().toString(), true, false, true);
        linkElement.setCondition(join.getOnExpression().toString());
        linkTable.getItems().add(linkElement);
        items.add(join.getRightItem().toString());
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
        getSelectBody().getSelectItems().add(nSItem);

        addRowToGroup(name);
    }

    @FXML
    public void deleteFIeldRow() {
        int selectedItem = fieldTable.getSelectionModel().getSelectedIndex();
        fieldTable.getItems().remove(selectedItem);
        getSelectBody().getSelectItems().remove(selectedItem);
    }

    private PlainSelect getSelectBody() {
        SelectBody selectBody;
        if (withItemMap.size() == 0) {
            selectBody = sQuery.getSelectBody();
        } else {
            Tab tab = cteTabPane.getSelectionModel().selectedItemProperty().get();
            selectBody = sQuery.getWithItemsList().get(withItemMap.get(tab.getId())).getSelectBody();
        }
        return (PlainSelect) selectBody;
    }

    public void addCteTableToForm(Tab tap) {
        qbTabPane_All.getTabs().add(tap);
        queryBatchTable.getItems().add("newwww");
    }

    @FXML
    public void onClickMethod() {
        queryBuilder.closeForm(sQuery.toString());
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
//                    setContextMenu();
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
//            System.out.println("deleteContext");
//            Object item = tablesView.getSelectionModel().getSelectedItem();
//            System.out.println("Selected item: " + item);
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
        ObservableList<LinkElement> data = linkTable.getItems();
        data.add(new LinkElement("", "", false, false, false));
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

        linkTableColumnTable1.setCellFactory(ComboBoxTableCell.forTableColumn(items));
        linkTableColumnTable2.setCellFactory(ComboBoxTableCell.forTableColumn(items));

        linkTableJoinCondition.setCellValueFactory(features -> new ReadOnlyObjectWrapper(features.getValue()));
        linkTableJoinCondition.setCellFactory(column -> new TableCell<LinkElement, LinkElement>() {

            private final ObservableList<String> langs = FXCollections.observableArrayList(items);
            private final ComboBox<String> langsComboBox = new ComboBox<>(langs);

            private final ObservableList<String> langs2 = FXCollections.observableArrayList("=", "<>", "<", ">", "<=", ">=");
            private final ComboBox<String> langsComboBox2 = new ComboBox<>(langs2);

            private final ObservableList<String> langs3 = FXCollections.observableArrayList(items);
            private final ComboBox<String> langsComboBox3 = new ComboBox<>(langs3);

            private final HBox pane = new HBox(langsComboBox, langsComboBox2, langsComboBox3);

            private final TextField ttt = new TextField();

            {
                langsComboBox.prefWidthProperty().bind(pane.widthProperty());
                langsComboBox2.setMinWidth(70);
                langsComboBox3.prefWidthProperty().bind(pane.widthProperty());
                ttt.setMaxWidth(Double.MAX_VALUE);
            }

            @Override
            protected void updateItem(LinkElement item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else if (item != null && item.isCustom()) {
                    ttt.setText(item.getCondition());
                    setGraphic(ttt);
                } else {
                    String condition = item.getCondition();
                    if (condition != null) {
                        String[] array = condition.split("[>=<=<>]");
                        langsComboBox.setValue(array[0]);
                        langsComboBox2.setValue(condition.replace(array[0], "").replace(array[1], ""));
                        langsComboBox3.setValue(array[1]);
                    }
                    setGraphic(pane);
                }
            }
        });
    }

    @FXML
    private TreeTableView<String> groupFieldsTree;
    @FXML
    private TreeTableColumn<String, String> groupFieldsTreeColumn;

    private void addRowToGroup(String name) {
        groupFieldsTree.getRoot().getChildren().add(new TreeItem<>(name));
    }

    @FXML
    private TreeTableView<String> conditionsTreeTable;
    @FXML
    private TreeTableColumn<String, String> conditionsTreeTableColumn;


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
}
