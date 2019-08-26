package com.querybuilder.home.lab;

import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainController {
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
    List<Tab> ggg;
    private Select sQuery;

    public TableView<String> queryBatchTable;
    @FXML
    private TableView<String> queryCteTable;
    @FXML
    private TableColumn<String, String> queryCteColumn;
    private Map<String, Integer> withItemMap;

    @FXML
    private TreeTableColumn<String, String> tableColumn1;

    protected MainAction mAction;

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
        mainTabPane.getSelectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) -> {
            cteTabPane.setVisible(newTab.getId() == null || !newTab.getId().equals("queryTabPane"));
        });

        fieldColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()));
        tableColumn1.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getValue()));
        queryCteColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()));

        databaseView.setOnMousePressed(e -> {
            if (e.getClickCount() == 2 && e.isPrimaryButtonDown()) {
                TreeItem<String> selectedItem = databaseView.getSelectionModel().getSelectedItem();
                String parent = selectedItem.getParent().getValue();
                String field = selectedItem.getValue();
                if ("Tables".equals(parent)) {

                } else {
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
        fieldTable.getItems().clear();
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
//            for (Table table : pSelect.getFromItem()) {
//                fieldTable.getItems().add(table.toString());
//            }
            for (Object select : pSelect.getSelectItems()) {
                fieldTable.getItems().add(select.toString());
            }
        }
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


}
