package com.querybuilder.controllers;

import com.querybuilder.QueryBuilder;
import com.querybuilder.database.DBStructure;
import com.querybuilder.database.DBStructureImpl;
import com.querybuilder.domain.CteRow;
import com.querybuilder.domain.DBTables;
import com.querybuilder.domain.SelectedFieldsTree;
import com.querybuilder.domain.qparts.FullQuery;
import com.querybuilder.domain.qparts.OneCte;
import com.querybuilder.domain.qparts.Union;
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

import static com.querybuilder.utils.Constants.CTE_0;
import static com.querybuilder.utils.Constants.UNION_0;
import static com.querybuilder.utils.Utils.activateNewTab;

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
    private TableView<CteRow> queryCteTable;
    @FXML
    private TableColumn<String, String> queryCteColumn;
    @FXML
    private Tab linkTablesPane;

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
            firstLoadQuery();
            return;
        }

        this.sQuery = sQuery;

        initDBTables();
        firstLoadQuery();
        setPagesListeners();
    }

    //<editor-fold desc="FILL SHOW QUERY">

    private boolean notChangeUnion;

    private void setPagesListeners() {
        cteTabPane.getSelectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) -> {
            if (oldTab == null || newTab == null) {
                return;
            }
            saveLoadPart(oldTab, null, newTab, null, true);
            loadUnionPages(newTab);
        });
        unionTabPane.getSelectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) -> {
            if (oldTab == null || newTab == null) {
                return;
            }
            Tab curCte = null;
            if (cteTabPane.getTabs().size() > 1) {
                curCte = cteTabPane.getSelectionModel().getSelectedItem();
            }
            saveLoadPart(curCte, oldTab, curCte, newTab, false);
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

    private String getCurrentUnion() {
        Tab selectedItem = unionTabPane.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            return UNION_0;
        }
        return selectedItem.getId();
    }

    private void loadUnionPages(Tab newTab) {
        unionTabPane.getTabs().clear();
        for (String union : fullQuery.getCteMap().get(newTab.getId()).getUnionMap().keySet()) {
            unionAliasesController.addUnionTabPane(union, union);
        }
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

    private int getCteTabIndex(String tabId) {
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

    FullQuery fullQuery;

    private void firstLoadQuery() {
        unionTabPane.getTabs().clear();
        cteTabPane.getTabs().clear();
        unionAliasesController.addUnionTabPane(UNION_0, UNION_0);

        fullQuery = new FullQuery();
        List<WithItem> withItemsList = sQuery.getWithItemsList();
        int index = 0;
        if (withItemsList != null) {
            for (WithItem x : withItemsList) {
                loadQueryPart(x.getName(), index, x.getSelectBody());
                index++;
            }
        }
        loadQueryPart("CTE_" + index, index, sQuery.getSelectBody());

        showFromBuilder(null, null, true);
    }

    private void saveLoadPart(Tab oldCte, Tab oldUnion, Tab newCte, Tab newUnion, boolean cteChange) {
        if (!withoutSave) {
            saveToBuilder(oldCte, oldUnion, cteChange);
        }
        showFromBuilder(newCte, newUnion, cteChange);
    }

    private void saveToBuilder(Tab oldCte, Tab oldUnion, boolean cteChange) {
        OneCte cte = getCurrentCte(oldCte);
        if (cteChange) {
            cte.saveAliasTable(unionAliasesController);
        }
        Union union = getCurrentUnion(cte, oldUnion, cteChange);
        union.saveFrom(tableFieldsController);
        union.saveLink(linksController);
        union.saveGroupBy(groupingController);
    }

    private void showFromBuilder(Tab newCte, Tab newUnion, boolean cteChange) {
        OneCte cte = getCurrentCte(newCte);
        if (cteChange) {
            cte.showAliasTable(unionAliasesController);
            CTEPart.load(this);
        }
        Union union = getCurrentUnion(cte, newUnion, false);
        union.showFrom(tableFieldsController);
        union.showLinks(linksController);
        TreeHelpers.load(this, union);
        union.showGroupBy(groupingController);
        TreeHelpers.cleanTrees(union);
    }

    private OneCte getCurrentCte(Tab newCte) {
        String cteId = CTE_0;
        if (newCte != null) {
            cteId = newCte.getId();
        }
        return fullQuery.getCteMap().get(cteId);
    }

    private Union getCurrentUnion(OneCte oneCte, Tab newUnion, boolean cteChange) {
        String unionId = UNION_0;
        if (cteChange) {
            unionId = getCurrentUnion();
        } else if (newUnion != null) {
            unionId = newUnion.getId();
        }

        return oneCte.getUnionMap().get(unionId);
    }

    private void loadQueryPart(String cteName, int id, SelectBody selectBody) {
        OneCte oneCte = loadCteData(cteName, selectBody);
        addCteTabPane(cteName, "CTE_" + id);
        queryCteTable.getItems().add(new CteRow(cteName, "CTE_" + id));
        curMaxCTE++;

        int index = 0;
        Map<String, Union> unionMap = oneCte.getUnionMap();

        if (selectBody instanceof PlainSelect) {
            loadUnionData(unionMap.get(UNION_0), (PlainSelect) selectBody);
        } else if (selectBody instanceof SetOperationList) {
            for (SelectBody union : ((SetOperationList) selectBody).getSelects()) {
                String key = "UNION_" + index;

                if (index > 0) {
                    Tab tab = new Tab(key);
                    tab.setId(key);
                    unionTabPane.getTabs().add(tab);
                }
                loadUnionData(unionMap.get(key), (PlainSelect) union);
                index++;
            }

        }
        oneCte.setUnionMap(unionMap);

        fullQuery.getCteMap().put(cteName, oneCte);
    }

    private OneCte loadCteData(String cteName, SelectBody selectBody) {
        OneCte oneCte = new OneCte();
        oneCte.setCteName(cteName);

        unionAliasesController.loadAliases(selectBody, oneCte);

        return oneCte;
    }

    private void loadUnionData(Union union, PlainSelect selectBody) {
        union.setTablesView(tableFieldsController.loadFromTables(selectBody));
        union.setLinkTable(linksController.loadLinks(selectBody));
        union.setGroupTableResults(groupingController.loadGroupBy(selectBody));
    }

    @FXML
    public void okClick() {
        saveToBuilder(
                cteTabPane.getSelectionModel().getSelectedItem(),
                unionTabPane.getSelectionModel().getSelectedItem(),
                true
        );
        sQuery = fullQuery.getQuery();
        queryBuilder.closeForm(sQuery.toString());
    }

    @FXML
    public void cancelClick(ActionEvent actionEvent) {
        queryBuilder.closeForm();
    }

    private boolean withoutSave;

    @FXML
    public void removeCTEClick(ActionEvent actionEvent) {
        if (queryCteTable.getItems().size() == 1) {
            return;
        }
        CteRow selectedItem = queryCteTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            return;
        }
        if (selectedItem.getId().equals(getCurrentCTE())) {
            withoutSave = true;
            int index = getCteTabIndex(selectedItem.getId()) - 1;
            cteTabPane.getSelectionModel().select(index == -1 ? 0 : index);
            withoutSave = false;
        }
        queryCteTable.getItems().remove(selectedItem);
        fullQuery.getCteMap().remove(selectedItem.getId());

        cteTabPane.getTabs().remove(getTabById(selectedItem.getId()));
    }

    private Tab getTabById(String tabId) {
        for (Tab tPane : cteTabPane.getTabs()) {
            if (tPane.getId().equals(tabId)) {
                return tPane;
            }
        }
        return null;
    }

    private int curMaxCTE; // индекс максимального СTE, нумерация начинается с 0

    @FXML
    private Tab tableAndFieldsTab;

    @FXML
    public void addCTEClick(ActionEvent actionEvent) {
        String key = "CTE_" + curMaxCTE;
        fullQuery.getCteMap().put(key, new OneCte(this));

        Tab newTab = addCteTabPane(key, key);
        activateNewTab(newTab, cteTabPane, this);

        queryCteTable.getItems().add(new CteRow(key, key));

        curMaxCTE++;
    }

    private Tab addCteTabPane(String name, String id) {
        Tab tab = new Tab(name);
        tab.setId(id);
        cteTabPane.getTabs().add(tab);
        return tab;
    }

    public String getCurrentCTE() {
        Tab selectedItem = cteTabPane.getSelectionModel().getSelectedItem();
        return selectedItem == null ? CTE_0 : selectedItem.getId();
    }
}