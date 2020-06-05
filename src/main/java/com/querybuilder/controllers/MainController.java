package com.querybuilder.controllers;

import com.querybuilder.QueryBuilder;
import com.querybuilder.database.DBStructure;
import com.querybuilder.database.DBStructureImpl;
import com.querybuilder.domain.DBTables;
import com.querybuilder.domain.SelectedFieldsTree;
import com.querybuilder.domain.TableRow;
import com.querybuilder.eventbus.Subscriber;
import com.querybuilder.querypart.*;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import lombok.Data;
import net.sf.jsqlparser.statement.select.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.querybuilder.utils.Utils.*;

@Data
public class MainController implements Subscriber {
    private Select sQuery;
    protected QueryBuilder queryBuilder;
    private Map<String, List<String>> dbElements;

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

    @FXML
    private Links linksController;
    @FXML
    private FromTables tableFieldsController;
    @FXML
    private UnionAliases unionAliasesController;
    @FXML
    private Conditions conditionsController;
    @FXML
    private GroupBy groupingController;
    @FXML
    private OrderBy orderController;

    private List<AbstractQueryPart> queryParts = new ArrayList<>();

    @FXML
    public void initialize() {
        fillQueryParts();
        queryParts.forEach(part -> {
            try {
                part.setMainController(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        CTEPart.init(this);
    }

    private void fillQueryParts() {
        queryParts.add(tableFieldsController);
        queryParts.add(linksController);
        queryParts.add(unionAliasesController);
        queryParts.add(conditionsController);
        queryParts.add(groupingController);
        queryParts.add(orderController);
    }

    @Override
    public void initData(Map<String, Object> userData) {
        this.queryBuilder = (QueryBuilder) userData.get("queryBuilder");
        Select sQuery = (Select) userData.get("sQuery");
        if (this.sQuery != null) {
            this.sQuery = sQuery;
            loadQuery();
            return;
        }

        this.sQuery = sQuery;

        initDBTables();
        loadQuery();
        setPagesListeners();
    }

    //<editor-fold desc="FILL SHOW QUERY">

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
        queryParts.forEach(part -> {
            try {
                part.save(selectBody);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        processUnionsAndCTE(cteTab, unionTab, selectBody);
    }

    private void processUnionsAndCTE(Tab cteTab, Tab unionTab, PlainSelect newSelectBody) {
        int unionNumber;
        if (unionTab == null) {
            int selectedIndex = unionTabPane.getSelectionModel().getSelectedIndex();
            unionNumber = (selectedIndex == -1 ? 0 : selectedIndex);
        } else {
            unionNumber = getTabIndex(this, unionTab.getId());
        }

        int cteNumber;
        if (cteTab == null) {
            int selectedIndex = cteTabPane.getSelectionModel().getSelectedIndex();
            cteNumber = (selectedIndex == -1 ? 0 : selectedIndex);
        } else {
            cteNumber = getCteTabId(cteTab.getId());
        }

        if (sQuery.getWithItemsList() != null && !sQuery.getWithItemsList().isEmpty()) {
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
            unionAliasesController.saveAliases(newSelectBody);
            if (selectBody instanceof SetOperationList) {
                ((SetOperationList) selectBody).getSelects().set(unionNumber, newSelectBody);
            } else {
                sQuery.setSelectBody(newSelectBody);
            }
        }
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

    public SelectBody getFullSelectBody() {
        SelectBody selectBody;
        int cteNumber = cteTabPane.getSelectionModel().getSelectedIndex();
        if (sQuery.getWithItemsList() == null || cteNumber == sQuery.getWithItemsList().size()) {
            selectBody = sQuery.getSelectBody();
        } else {
            selectBody = sQuery.getWithItemsList().get(cteNumber).getSelectBody();
        }
        return selectBody;
    }

    public void loadCurrentQuery(boolean firstRun) {
        int cteNumber = cteTabPane.getSelectionModel().getSelectedIndex();
        int unionNumber;
        boolean cteChange = (cteNumberPrev != cteNumber);

        clearTables(cteChange, firstRun);
        SelectBody selectBody = getFullSelectBody();

        if (cteChange || firstRun) {
            unionTabPane.getTabs().clear();
        }

        if (selectBody instanceof SetOperationList) { // UNIONS LOAD
            SetOperationList setOperationList = (SetOperationList) selectBody;
            if (cteNumberPrev != cteNumber || firstRun) {
                unionAliasesController.loadUnionTabPanes(setOperationList);
            }
            unionNumber = unionTabPane.getSelectionModel().getSelectedIndex();
            SelectBody body = setOperationList.getSelects().get(unionNumber == -1 ? 0 : unionNumber);
            loadSelectData((PlainSelect) body);
            if (!firstRun) {
                unionAliasesController.setAliasesIds();
            }
        } else if (selectBody instanceof PlainSelect) { // ONE QUERY
            loadSelectData((PlainSelect) selectBody);
        } else if (selectBody == null) { // new empty query
            TreeHelpers.load(this);
        }

        if (cteChange || firstRun) {
            try {
                // таблица Alias меняется только при переключении CTE
                unionAliasesController.loadAliases(selectBody);
                CTEPart.load(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        cteNumberPrev = cteNumber;
    }


    private void initDBTables() {
        DBStructure db = new DBStructureImpl();
        DBTables dbStructure = db.getDBStructure(queryBuilder.getConsole());
        dbElements = dbStructure.getDbElements();
        tableFieldsController.loadDbStructureToTree(dbStructure);
    }

    //</editor-fold>

    private SelectedFieldsTree selectedGroupFieldsTree;
    private SelectedFieldsTree selectedConditionsTreeTable;
    private SelectedFieldsTree selectedOrderFieldsTree;

    public void refreshLinkTable() {
        linksController.getLinkTable().refresh();
    }

    private void loadQuery() {
        unionTabPane.getTabs().clear();
        cteTabPane.getTabs().clear();
        queryCteTable.getItems().clear();
        //unionAliasesController.setCurMaxUnion(1);
        curMaxCTE = 1;

        // one query
        List<WithItem> withItemsList = sQuery.getWithItemsList();
        if (withItemsList == null) {
            loadCurrentQuery(true);
            queryCteTable.getItems().add("Query_1");
            return;
        }

        // CTE
        for (WithItem x : withItemsList) {
            String cteName = x.getName();
            Tab tab = new Tab(cteName);
            tab.setId(cteName);
            cteTabPane.getTabs().add(tab);
            queryCteTable.getItems().add(cteName);
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
        queryParts.forEach(part -> {
            try {
                part.load(pSelect);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void clearIfNotNull(TreeTableView<TableRow> treeTable) {
        if (treeTable.getRoot() != null && treeTable.getRoot().getChildren() != null) {
            treeTable.getRoot().getChildren().clear();
        }
    }

    private void clearTables(boolean cteChange, boolean firstRun) {
        tableFieldsController.getFieldTable().getItems().clear();
        tableFieldsController.getTablesView().getRoot().getChildren().clear();

        if (!firstRun) {
            clearIfNotNull(conditionsController.getConditionsTreeTable());
            clearIfNotNull(groupingController.getGroupFieldsTree());
            clearIfNotNull(orderController.getOrderFieldsTree());
        }

        conditionsController.getConditionTableResults().getItems().clear();
        groupingController.getGroupTableResults().getItems().clear();
        groupingController.getGroupTableAggregates().getItems().clear();
        orderController.getOrderTableResults().getItems().clear();

        if (cteChange) {
            unionAliasesController.getUnionTable().getItems().clear();
//            aliasTable.getItems().clear();
        }
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
    public void cancelClick(ActionEvent actionEvent) {
        queryBuilder.closeForm();
    }

    @FXML
    public void onDBTableChange() {
//        System.out.println("234");
    }


    //    public void insertResult(String result, TableRow item, SubSelect subSelect) {
////        item.setQuery(result);
////        PlainSelect selectBody = getSelectBody();
////        if (selectBody.getFromItem().getAlias() != null && selectBody.getFromItem().getAlias().getName().equals(item.getName())) {
////            selectBody.setFromItem(subSelect);
////        } else {
////            selectBody.getJoins().forEach((x) -> {
////                if (x.getRightItem().getAlias() != null && x.getRightItem().getAlias().getName().equals(item.getName())) {
////                    x.setRightItem(subSelect);
////                }
////            });
////        }
////        System.out.println(selectBody);
//    }
//
    @FXML
    public void removeCTEClick(ActionEvent actionEvent) {
    }

    private int curMaxCTE; // индекс максимального СTE, нумерация начинается с 0

    @FXML
    private Tab tableAndFieldsTab;

    @FXML
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

        activateNewTab(newTab, cteTabPane, this);
    }

    private void addCte(List<WithItem> newItemsList, String lastQuery) {
        WithItem wItem = new WithItem();
        wItem.setName(lastQuery);
        wItem.setSelectBody(sQuery.getSelectBody());
        newItemsList.add(wItem);
    }

    private Tab addCteTabPane(int curMaxCTE) {
        String cteName = "Query_" + curMaxCTE;
        Tab tab = new Tab(cteName);
        tab.setId(cteName);
        cteTabPane.getTabs().add(tab);
        return tab;
    }
}