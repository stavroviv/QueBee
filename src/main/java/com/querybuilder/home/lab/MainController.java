package com.querybuilder.home.lab;

import com.intellij.database.model.ObjectKind;
import com.intellij.database.psi.DbDataSource;
import com.intellij.database.psi.DbDataSourceImpl;
import com.intellij.database.util.DbUtil;
import com.intellij.database.vfs.ObjectPath;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.util.SmartList;
import com.intellij.util.containers.JBIterable;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


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

    public MainController(Select sQuery) {
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
        databaseView.setRoot(fillDatabaseTables());
        mainTabPane.getSelectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) -> {
            cteTabPane.setVisible(newTab.getId() == null || !newTab.getId().equals("queryTabPane"));
        });

        fieldColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()));
        tableColumn1.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getValue()));
        queryCteColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()));


        databaseView.setOnMousePressed(e -> {
            if (e.getClickCount() == 2 && e.isPrimaryButtonDown()) {
                int index = databaseView.getSelectionModel().getSelectedIndex();
                System.out.println("" + index);
            }
        });

        reloadData();
        cteTabPane.getSelectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) -> {
            showCTE(withItemMap.get(newTab.getId()));
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
        fieldTable.getItems().add("test");
    }

    @FXML
    public void deleteFIeldRow() {
        String selectedItem = fieldTable.getSelectionModel().getSelectedItem();
        fieldTable.getItems().remove(selectedItem);
    }

    public void addCteTableToForm(Tab tap) {
        qbTabPane_All.getTabs().add(tap);
        queryBatchTable.getItems().add("newwww");
    }

    @FXML
    public void onClickMethod() {

    }

    @FXML
    public void onDBTableChange() {
        System.out.println("234");
    }

    @FXML
    public void onCancelClickMethod(ActionEvent actionEvent) {
        MainAction.clos();
    }

    private TreeItem<String> fillDatabaseTables() {
        Project p = ProjectManager.getInstance().getOpenProjects()[0];
        JBIterable<DbDataSource> dataSources = DbUtil.getDataSources(p);
        DbDataSourceImpl dbDataSource = (DbDataSourceImpl) dataSources.get(0);

        TreeItem<String> root = new TreeItem<>("Tables");
        root.setExpanded(true);

        try {
            Field myQNamesField = dbDataSource.getClass().getDeclaredField("myQNames");
            myQNamesField.setAccessible(true);
            Map<Object, Object> myQNames = (Map<Object, Object>) myQNamesField.get(dbDataSource);

            SmartList list = (SmartList) myQNames.get(ObjectPath.create("socnet", ObjectKind.SCHEMA));
            Object myTables = list.get(0);
            Field field2 = myTables.getClass().getDeclaredField("myTables");
            field2.setAccessible(true);

            Object value222 = field2.get(myTables);

            Field myElements = value222.getClass().getSuperclass().getSuperclass().getDeclaredField("myElements");
            myElements.setAccessible(true);

            List valueFinal = (List) myElements.get(value222);
            valueFinal.forEach(x ->
                    {
                        try {
                            Field myNameField = x.getClass().getSuperclass().getDeclaredField("myName");
                            myNameField.setAccessible(true);
                            String myName = (String) myNameField.get(x);
                            TreeItem<String> stringTreeItem = new TreeItem<>(myName);
                            root.getChildren().add(stringTreeItem);

                            Field myColumnsField = x.getClass().getDeclaredField("myColumns");
                            myColumnsField.setAccessible(true);
                            Object myColumns = myColumnsField.get(x);


                            Field myColumnsElementsField = myColumns.getClass().getSuperclass().getSuperclass().getSuperclass().getDeclaredField("myElements");
                            myColumnsElementsField.setAccessible(true);
                            List myColumnElements = (List) myColumnsElementsField.get(myColumns);
                            myColumnElements.forEach(xx -> {
                                try {
                                    Field myColName = xx.getClass().getSuperclass().getDeclaredField("myName");
                                    myColName.setAccessible(true);
                                    String myColNameStr = (String) myColName.get(xx);
                                    stringTreeItem.getChildren().add(new TreeItem<>(myColNameStr));
                                } catch (NoSuchFieldException | IllegalAccessException e) {
                                    e.printStackTrace();
                                }
                            });
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
            );

        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return root;
    }
}
