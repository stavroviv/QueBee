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
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.WithItem;

import java.lang.reflect.Field;
import java.util.ArrayList;
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

    public TableView<String> queryBatchTable;

    @FXML
    private TreeTableColumn<String, String> tableColumn1;

    public MainController( Select sQuery) {
//        this.selectItems = selectItems;

        List<WithItem> withItemsList = sQuery.getWithItemsList();
        PlainSelect selectBody = (PlainSelect) sQuery.getSelectBody();
        selectItems = selectBody.getSelectItems();

    }

    public void initialize() {

        databaseView.setRoot(fillDatabaseTables());
        mainTabPane.getSelectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) -> {
            cteTabPane.setVisible(newTab.getId() == null || !newTab.getId().equals("queryTabPane"));
        });

        fieldColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()));
        tableColumn1.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getValue()));


//        for (int i = 0; i < 100 ; i++) {
//            cteTabPane.getTabs().add(new Tab("test"));
//        }
    }

//    public Tab addQueryCTE(TreeItem<String> root) {
//        FXMLLoader fxmlLoader1 = new FXMLLoader(getClass().getResource("/builder-forms/main-tabpane-tab.fxml"));
//        fxmlLoader1.setController(new FormController(root, this));
//        Parent root2 = null;
//        try {
//            root2 = fxmlLoader1.load();
//        } catch (Exception e1) {
//            e1.printStackTrace();
//        }
//        Tab tt = new Tab("Tab 2", root2);
//        return tt;
//    }

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
