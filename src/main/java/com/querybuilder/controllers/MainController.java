package com.querybuilder.controllers;

import com.querybuilder.QueryBuilder;
import com.querybuilder.database.DBStructure;
import com.querybuilder.database.DBStructureImpl;
import com.querybuilder.domain.TableRow;
import com.querybuilder.domain.*;
import com.querybuilder.eventbus.CustomEvent;
import com.querybuilder.eventbus.CustomEventBus;
import com.querybuilder.eventbus.Subscriber;
import com.querybuilder.querypart.*;
import com.querybuilder.utils.Utils;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.AnchorPane;
import net.engio.mbassy.listener.Handler;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.querybuilder.controllers.SelectedFieldController.FIELD_FORM_CLOSED_EVENT;
import static com.querybuilder.utils.Constants.*;
import static com.querybuilder.utils.Utils.*;

public class MainController implements Subscriber {

    @FXML
    private Button cancelButton;

    @FXML
    private TableView<TableRow> fieldTable;

    public TableView<TableRow> getFieldTable() {
        return fieldTable;
    }

    @FXML
    private TableColumn<TableRow, String> fieldColumn;

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

    @FXML
    private TableView<String> queryCteTable;
    @FXML
    private TableColumn<String, String> queryCteColumn;

    protected QueryBuilder queryBuilder;

    private Map<String, List<String>> dbElements;

    public Map<String, List<String>> getDbElements() {
        return dbElements;
    }

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
        initQueryParts();
        reloadData();

        setCellFactories();
        setPagesListeners();

        CustomEventBus.register(this);
    }

    //<editor-fold defaultstate="collapsed" desc="FILL SHOW AUERY">

    private boolean notChangeUnion;

    private void setPagesListeners() {
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
        PlainSelect selectBody = getEmptySelect();
        try {
            FromTables.save(this, selectBody);
            SelectedFields.save(this, selectBody);
            Links.save(this, selectBody);
            GroupBy.save(this, selectBody);
            OrderBy.save(this, selectBody);
            Conditions.save(this, selectBody);
        } catch (Exception e) {
            e.printStackTrace();
        }

        processUnionsAndCTE(cteTab, unionTab, selectBody);
    }

    private void processUnionsAndCTE(Tab cteTab, Tab unionTab, PlainSelect newSelectBody) {
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
            try {
                loadAliasTable(selectBody);
                loadCteTables();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        if (selectBody instanceof SetOperationList) { // UNION
            SetOperationList setOperationList = (SetOperationList) selectBody;
            if (cteNumberPrev != cteNumber || firstRun) {
                for (int i = 1; i <= setOperationList.getSelects().size(); i++) {
                    addUnion("Query " + i, i - 1);
                }
                curMaxUnion = setOperationList.getSelects().size() - 1;
            }
            unionNumber = unionTabPane.getSelectionModel().getSelectedIndex();
            SelectBody body = setOperationList.getSelects().get(unionNumber == -1 ? 0 : unionNumber);
            loadSelectData((PlainSelect) body);
        } else if (selectBody instanceof PlainSelect) { // ONE QUERY
            loadSelectData((PlainSelect) selectBody);
        }

        initSelectedTables(); // must be after load tables
        cteNumberPrev = cteNumber;
    }

    private void loadCteTables() {
        // загрузить в дерево таблиц предыдущие CTE
        TreeItem<TableRow> root = databaseTableView.getRoot();
        root.getChildren().forEach(item -> {
            if (item.getValue().isCte()) {
                root.getChildren().remove(item);
            }
        });

        List<WithItem> withItemsList = sQuery.getWithItemsList();
        if (withItemsList == null) {
            return;
        }

        int i = 0;
        int currentCTE = cteTabPane.getSelectionModel().getSelectedIndex();
        if (currentCTE == 0) {
            return;
        }

        TableRow cteRoot = new TableRow(CTE_ROOT);
        cteRoot.setCteRoot(true);
        cteRoot.setCte(true);
        TreeItem<TableRow> cteRootItem = new TreeItem<>(cteRoot);
        cteRootItem.setExpanded(true);
        root.getChildren().add(0, cteRootItem);

        for (WithItem withItem : withItemsList) {
            if (i == currentCTE) {
                break;
            }
            String cteName = withItem.getName();
            TableRow tableRow = new TableRow(cteName);
            tableRow.setRoot(true);
            tableRow.setCte(true);
            TreeItem<TableRow> treeItem = new TreeItem<>(tableRow);
            cteRootItem.getChildren().add(treeItem);
            if (withItem.getSelectBody() instanceof PlainSelect) {
                PlainSelect selectBody = (PlainSelect) withItem.getSelectBody();
                List<SelectItem> selectItems = selectBody.getSelectItems();
                selectItems.forEach(item -> {
                    SelectExpressionItem selectItem = (SelectExpressionItem) item;
                    String name;
                    if (selectItem.getAlias() != null) {
                        name = selectItem.getAlias().getName();
                    } else {
                        Column column = (Column) selectItem.getExpression();
                        String[] split = column.getColumnName().split("\\.");
                        name = split.length > 1 ? split[1] : split[0]; // FIXME
                    }
                    treeItem.getChildren().add(new TreeItem<>(new TableRow(name)));
                });
            }
            i++;
        }
    }

    private void loadAliasTable(SelectBody selectBody) {
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
        for (Object x : selectItems) {
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

        TreeItem<TableRow> dbStructure = db.getDBStructure(this.queryBuilder.getDataSource());
        databaseTableView.setRoot(new TreeItem<>());
        databaseTableView.getRoot().getChildren().add(dbStructure);

        dbElements = db.getDbElements();
        tablesView.setRoot(new TreeItem<>());
    }

    //</editor-fold>

    @FXML
    private TreeTableView<TableRow> databaseTableView;

    public TreeTableView<TableRow> getDatabaseTableView() {
        return databaseTableView;
    }

    @FXML
    private TreeTableColumn<TableRow, TableRow> databaseTableColumn;

    private void initQueryParts() {
        FromTables.init(this);
        setResultsTablesHandlers();
        Links.init(this);
        Conditions.init(this);
        initAliasTable();
    }

    private SelectedFieldsTree selectedGroupFieldsTree;

    public SelectedFieldsTree getSelectedGroupFieldsTree() {
        return selectedGroupFieldsTree;
    }

    private SelectedFieldsTree selectedConditionsTreeTable;

    public SelectedFieldsTree getSelectedConditionsTreeTable() {
        return selectedConditionsTreeTable;
    }

    private SelectedFieldsTree selectedOrderFieldsTree;

    public SelectedFieldsTree getSelectedOrderFieldsTree() {
        return selectedOrderFieldsTree;
    }

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
        setCellFactory(orderFieldsTreeColumn);
        setCellFactory(databaseTableColumn);
    }


    public void refreshLinkTable() {
        linkTable.refresh();
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
        newColumn.setCellFactory(x -> new AliasCell(x, i, getSeleсtedItems()));

        aliasTable.getSelectionModel().selectedIndexProperty().addListener((num) -> {
            TablePosition focusedCell = aliasTable.getFocusModel().getFocusedCell();
            aliasTable.edit(focusedCell.getRow(), focusedCell.getTableColumn());
        });

        aliasTable.getColumns().add(newColumn);
        unionColumns.put(unionName, newColumn);
        unionTable.getItems().add(new TableRow(unionName));
    }

    private List<String> getSeleсtedItems() {
        List<String> items = new ArrayList<>();
        fieldTable.getItems().forEach(x -> items.add(x.getName()));
        return items;
    }

    private void loadSelectData(PlainSelect pSelect) {
        try {
            FromTables.load(this, pSelect);
            SelectedFields.load(this, pSelect);
            Links.load(this, pSelect);
            GroupBy.load(this, pSelect);
            OrderBy.load(this, pSelect);
            Conditions.load(this, pSelect);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clearIfNotNull(TreeTableView<TableRow> treeTable) {
        if (treeTable.getRoot() != null && treeTable.getRoot().getChildren() != null) {
            treeTable.getRoot().getChildren().clear();
        }
    }

    private void clearTables(boolean cteChange, boolean firstRun) {
        fieldTable.getItems().clear();
        tablesView.getRoot().getChildren().clear();

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

    public void addFieldRow(String name) {
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
        refreshLinkTable();
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

    public TreeTableView<TableRow> getTablesView() {
        return tablesView;
    }

    @FXML
    private TreeTableColumn<TableRow, TableRow> tablesViewColumn;

    public TreeTableColumn<TableRow, TableRow> getTablesViewColumn() {
        return tablesViewColumn;
    }

    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="LINK TABLE">

    @FXML
    private TableView<LinkElement> linkTable;

    public TableView<LinkElement> getLinkTable() {
        return linkTable;
    }

    @FXML
    private TableColumn<LinkElement, LinkElement> linkTableColumnTable1;

    public TableColumn<LinkElement, LinkElement> getLinkTableColumnTable1() {
        return linkTableColumnTable1;
    }

    @FXML
    private TableColumn<LinkElement, LinkElement> linkTableColumnTable2;

    public TableColumn<LinkElement, LinkElement> getLinkTableColumnTable2() {
        return linkTableColumnTable2;
    }

    @FXML
    private TableColumn<LinkElement, Boolean> linkTableAllTable1;

    public TableColumn<LinkElement, Boolean> getLinkTableAllTable1() {
        return linkTableAllTable1;
    }

    @FXML
    private TableColumn<LinkElement, Boolean> linkTableAllTable2;

    public TableColumn<LinkElement, Boolean> getLinkTableAllTable2() {
        return linkTableAllTable2;
    }

    @FXML
    private TableColumn<LinkElement, Boolean> linkTableCustom;

    public TableColumn<LinkElement, Boolean> getLinkTableCustom() {
        return linkTableCustom;
    }

    @FXML
    private TableColumn<LinkElement, LinkElement> linkTableJoinCondition;

    public TableColumn<LinkElement, LinkElement> getLinkTableJoinCondition() {
        return linkTableJoinCondition;
    }

    @FXML
    private Tab linkTablesPane;

    @FXML
    protected void addLinkElement(ActionEvent event) {
        LinkElement newRow = new LinkElement(this);
        linkTable.getItems().add(newRow);
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

    public TreeTableView<TableRow> getConditionsTreeTable() {
        return conditionsTreeTable;
    }


    @FXML
    private TreeTableColumn<TableRow, TableRow> conditionsTreeTableColumn;

    @FXML
    private TableView<ConditionElement> conditionTableResults;

    public TableView<ConditionElement> getConditionTableResults() {
        return conditionTableResults;
    }

    @FXML
    private TableColumn<ConditionElement, Boolean> conditionTableResultsCustom;

    public TableColumn<ConditionElement, Boolean> getConditionTableResultsCustom() {
        return conditionTableResultsCustom;
    }

    @FXML
    private TableColumn<ConditionElement, ConditionElement> conditionTableResultsCondition;

    public TableColumn<ConditionElement, ConditionElement> getConditionTableResultsCondition() {
        return conditionTableResultsCondition;
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

    public void openNestedQuery(String text, TableRow item) {
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

    public void makeSelect(TreeItem<TableRow> selectedItem, TreeTableView<TableRow> fieldsTree,
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
            if (!parentName.equals(DATABASE_TABLE_ROOT)) {
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
        addElementBeforeTree(groupFieldsTree.getRoot().getChildren(), treeItem);
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

    public TreeTableView<TableRow> getGroupFieldsTree() {
        return groupFieldsTree;
    }

    @FXML
    private TreeTableColumn<TableRow, TableRow> groupFieldsTreeColumn;

    @FXML
    private TableView<TableRow> groupTableResults;

    public TableView<TableRow> getGroupTableResults() {
        return groupTableResults;
    }

    @FXML
    private TableColumn<TableRow, String> groupTableResultsFieldColumn;

    @FXML
    private TableView<TableRow> groupTableAggregates;

    public TableView<TableRow> getGroupTableAggregates() {
        return groupTableAggregates;
    }

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

    public TreeTableView<TableRow> getOrderFieldsTree() {
        return orderFieldsTree;
    }

    @FXML
    private TreeTableColumn<TableRow, TableRow> orderFieldsTreeColumn;

    @FXML
    private TableView<TableRow> orderTableResults;

    public TableView<TableRow> getOrderTableResults() {
        return orderTableResults;
    }

    @FXML
    private TableColumn<TableRow, String> orderTableResultsFieldColumn;
    @FXML
    private TableColumn<TableRow, String> orderTableResultsSortingColumn;

    private void setOrderHandlers() {
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
                },
                selectedIndex, orderTableResults.getItems())
        );
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

    private int curMaxUnion; // индекс максимального объединения, нумерация начинается с 0
    private Map<String, TableColumn> unionColumns;

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