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
import net.sf.jsqlparser.statement.select.SelectItem;

import java.lang.reflect.Field;
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
    private TreeTableView<String> databaseView;
    @FXML
    private TreeTableColumn<String, String> tableColumn1;

    @FXML
    private TableView<String> fieldTable;
    @FXML
    private TableColumn<String, String> fieldColumn;
    private List<SelectItem> selectItems;

    @FXML
    private Tab qbTabPane_1;
    @FXML
    private TabPane qbTabPane_All;

    public MainController(List<SelectItem> selectItems) {
        this.selectItems = selectItems;
    }

    public void initialize() {
        fillDatabaseTables();
//        fieldColumn.setCellValueFactory(new PropertyValueFactory<>("fieldd"));
        fieldColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()));
        selectItems.forEach(x -> fieldTable.getItems().add(x.toString()));


//
//        Tab tt = new Tab("Tab 2");
//        tt.setContent(qbTabPane_1.clone);
//        qbTabPane_All.getTabs().add(tt);
//        qbTabPane_All.getTabs().add(new Tab("Tab 3"));
//        qbTabPane_All.getTabs().add(qbTabPane_1);
//        qbTabPane_All.getTabs().add(qbTabPane_1);
    }

    @FXML
    public void onClickMethod() {

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

    @FXML
    public void onCancelClickMethod(ActionEvent actionEvent) {
        MainAction.clos();
    }

    private void fillDatabaseTables() {
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
            tableColumn1.setCellValueFactory(cellData ->
                    new SimpleStringProperty(cellData.getValue().getValue()));
            databaseView.setRoot(root);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

}
