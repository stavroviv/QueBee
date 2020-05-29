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
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import lombok.Data;
import net.engio.mbassy.listener.Handler;
import net.sf.jsqlparser.statement.select.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.querybuilder.controllers.SelectedFieldController.FIELD_FORM_CLOSED_EVENT;
import static com.querybuilder.querypart.UnionAliases.addUnionColumn;
import static com.querybuilder.utils.Constants.GROUP_DEFAULT_VALUE;
import static com.querybuilder.utils.Constants.ORDER_DEFAULT_VALUE;
import static com.querybuilder.utils.Utils.makeDeselect;
import static com.querybuilder.utils.Utils.makeSelect;

@Data
public class MainController implements Subscriber {
    private Select sQuery;
    protected QueryBuilder queryBuilder;
    private Map<String, List<String>> dbElements;

    @FXML
    private Button cancelButton;
    @FXML
    private TableView<TableRow> fieldTable;
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
    @FXML
    private Spinner<Integer> topSpinner;
    @FXML
    private TableView<String> queryCteTable;
    @FXML
    private TableColumn<String, String> queryCteColumn;

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
                UnionAliases.load(this, selectBody);
                CTEPart.load(this);
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
        } else if (selectBody == null) { // new empty query
            TreeHelpers.load(this);
        }

        cteNumberPrev = cteNumber;
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
    @FXML
    private TreeTableColumn<TableRow, TableRow> databaseTableColumn;

    private void initQueryParts() {
        FromTables.init(this);
        OrderBy.init(this);
        GroupBy.init(this);
        SelectedFields.init(this);
        Links.init(this);
        Conditions.init(this);
        UnionAliases.init(this);
        CTEPart.init(this);
    }

    private SelectedFieldsTree selectedGroupFieldsTree;
    private SelectedFieldsTree selectedConditionsTreeTable;
    private SelectedFieldsTree selectedOrderFieldsTree;

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

    private void loadSelectData(PlainSelect pSelect) {
        try {
            FromTables.load(this, pSelect);
            SelectedFields.load(this, pSelect);
            TreeHelpers.load(this); // must be after load tables
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
    @FXML
    private TreeTableColumn<TableRow, TableRow> tablesViewColumn;

    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="LINK TABLE">

    @FXML
    private TableView<LinkElement> linkTable;
    @FXML
    private TableColumn<LinkElement, LinkElement> linkTableColumnTable1;
    @FXML
    private TableColumn<LinkElement, LinkElement> linkTableColumnTable2;
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
    @FXML
    private TreeTableColumn<TableRow, TableRow> conditionsTreeTableColumn;
    @FXML
    private TableView<ConditionElement> conditionTableResults;
    @FXML
    private TableColumn<ConditionElement, Boolean> conditionTableResultsCustom;
    @FXML
    private TableColumn<ConditionElement, ConditionElement> conditionTableResultsCondition;

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

    @FXML
    private void editFieldClick() {
        SelectedFields.editField(this);
    }

    @FXML
    protected void selectOrder(ActionEvent event) {
        makeSelect(orderFieldsTree, orderTableResults, ORDER_DEFAULT_VALUE);
    }

    @FXML
    protected void deselectOrder(ActionEvent event) {
        makeDeselect(orderTableResults, orderFieldsTree);
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
    @FXML
    private TableView<AliasRow> aliasTable;
    @FXML
    private TableColumn<AliasRow, String> aliasFieldColumn;

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
        addUnionColumn(this, unionName, curMaxUnion);
        addUnion(unionName, curMaxUnion);
    }

    private void addFirstUnion() {
        addUnion("Query 1", 0);
        addUnion("Query 2", 1);
        addUnionColumn(this, "Query 2", curMaxUnion);
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