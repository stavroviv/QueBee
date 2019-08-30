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
    private TreeTableView<String> locationTreeView;
    @FXML
    private Button okButton;
    @FXML
    private Button cancelButton;
    @FXML
    private TableView<String> fieldTable;
    private List<SelectItem> selectItems;
    private List<SelectItem> cteList;
    @FXML
    private TreeTableView<String> databaseView;

    @FXML
    private TabPane qbTabPane_All;
    @FXML
    private TabPane mainTabPane;
    @FXML
    private TableColumn<String, String> fieldColumn;
    @FXML
    private TabPane cteTabPane;

    private Select sQuery;

    public TableView<String> queryBatchTable;
    @FXML
    private TableView<String> queryCteTable;
    @FXML
    private TableColumn<String, String> queryCteColumn;
    private Map<String, Integer> withItemMap;

    @FXML
    private TreeTableColumn<String, String> tableColumn1;

    @FXML
    private TreeTableColumn<String, String> tablesViewColumn;
    @FXML
    private TreeTableView<String> tablesView;

    protected MainAction mAction;
    private Map<String, List<String>> dbElements;

    private ObservableList<String> items;

    public MainController(Select sQuery, MainAction mAction) {
        this.mAction = mAction;
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
        databaseView.setRoot(db.getDBStructure());
        dbElements = db.getDbElements();
        items = FXCollections.observableArrayList();
        tablesView.setRoot(new TreeItem<>(TABLES_ROOT));
        groupFieldsTree.setRoot(new TreeItem<>(TABLES_ROOT));
        conditionsTreeTable.setRoot(new TreeItem<>(TABLES_ROOT));

        mainTabPane.getSelectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) -> {
            cteTabPane.setVisible(newTab.getId() == null || !newTab.getId().equals("queryTabPane"));
        });

        setCellFactories();

        databaseView.setOnMousePressed(e -> {
            if (e.getClickCount() == 2 && e.isPrimaryButtonDown()) {
                TreeItem<String> selectedItem = databaseView.getSelectionModel().getSelectedItem();
                String parent = selectedItem.getParent().getValue();
                String field = selectedItem.getValue();
                if (DATABASE_ROOT.equals(parent)) {
                    addTablesRow(field);
                } else {
                    addTablesRow(parent);
                    addFieldRow(parent + "." + field);
                }
            }
        });
        tablesView.setOnMousePressed(e -> {
            if (e.getClickCount() == 2 && e.isPrimaryButtonDown()) {
                TreeItem<String> selectedItem = tablesView.getSelectionModel().getSelectedItem();
                String parent = selectedItem.getParent().getValue();
                String field = selectedItem.getValue();
                if (!TABLES_ROOT.equals(parent)) {
                    addFieldRow(parent + "." + field);
                }
            }
        });
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
        initLinkTable();
    }

    private void setCellFactories() {
        // table columns
        fieldColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()));
        queryCteColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()));
        // tree columns
        tableColumn1.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getValue()));
        tablesViewColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getValue()));
        groupFieldsTreeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getValue()));
        conditionsTreeTableColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getValue()));
    }

    private void addTablesRow(String parent) {
        ObservableList<TreeItem<String>> children = tablesView.getRoot().getChildren();
        if (children.stream().noneMatch(x -> x.getValue().equals(parent))) {
            tablesView.getRoot().getChildren().add(getTableItemWithFields(parent));
            conditionsTreeTable.getRoot().getChildren().add(getTableItemWithFields(parent));
        }
    }

    private void reloadData() {
        int i = 0;
        cteTabPane.getTabs().clear();
        queryCteTable.getItems().clear();

        List<WithItem> withItemsList = sQuery.getWithItemsList();
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
        if (cteNumber == sQuery.getWithItemsList().size()) {
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
            conditionsTreeTable.getRoot().getChildren().add(getTableItemWithFields(table.getName()));
        }
        List<Join> joins = pSelect.getJoins();
        if (joins == null) {
            return;
        }
        linkTablesPane.setDisable(false);
        linkTable.getItems().clear();
        items.add(table.getName());
        for (Join join : joins) {
            tablesView.getRoot().getChildren().add(getTableItemWithFields(join.getRightItem().toString()));
            conditionsTreeTable.getRoot().getChildren().add(getTableItemWithFields(join.getRightItem().toString()));
            addLinkElement(table, join);
        }
    }

    private void addLinkElement(Table table, Join join) {
        LinkElement linkElement = new LinkElement(table.getName(), join.getRightItem().toString(), true, false, true);
        linkElement.setCondition(join.getOnExpression().toString());
        linkTable.getItems().add(linkElement);
        items.add(join.getRightItem().toString());
    }

    private TreeItem<String> getTableItemWithFields(String tableName) {
        TreeItem<String> stringTreeItem = new TreeItem<>(tableName);
        List<String> columns = dbElements.get(tableName);
        if (columns != null) {
            columns.forEach(col ->
                    {
                        stringTreeItem.getChildren().add(new TreeItem<>(col));
                        addRowToGroup(tableName + "." + col);
                    }
            );
        }
        return stringTreeItem;
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
        Tab tab = cteTabPane.getSelectionModel().selectedItemProperty().get();
        SelectBody selectBody = sQuery.getWithItemsList().get(withItemMap.get(tab.getId())).getSelectBody();
        return (PlainSelect) selectBody;
    }

    public void addCteTableToForm(Tab tap) {
        qbTabPane_All.getTabs().add(tap);
        queryBatchTable.getItems().add("newwww");
    }

    @FXML
    public void onClickMethod() {
        mAction.clos(sQuery.toString());
    }

    @FXML
    public void onDBTableChange() {
        System.out.println("234");
    }

    @FXML
    public void onCancelClickMethod(ActionEvent actionEvent) {
        mAction.clos();
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

    private void initLinkTable() {
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


}
