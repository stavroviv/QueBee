package com.querybuilder.home.lab;

import com.intellij.database.model.ObjectKind;
import com.intellij.database.psi.DbDataSource;
import com.intellij.database.psi.DbDataSourceImpl;
import com.intellij.database.util.DbUtil;
import com.intellij.database.vfs.ObjectPath;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.SmartList;
import com.intellij.util.containers.JBIterable;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

//import com.intellij.modules.*;

public class MainController {
    @FXML
    private TreeTableView<String> locationTreeView;
//    @FXML
//    private TableColumn<Person, String> dbName;

    @FXML
    private Button okButton;
    @FXML
    private TreeTableView<String> databaseView;
    @FXML
    private TreeTableColumn<String, String> tableColumn1;

    // the initialize method is automatically invoked by the FXMLLoader - it's magic
    public void initialize() {
        loadTreeItems("initial 1", "initial 2", "initial 3");
    }

    @FXML
    public void onClickMethod() {
        Project p = ProjectManager.getInstance().getOpenProjects()[0];
        JBIterable<DbDataSource> dataSources = DbUtil.getDataSources(p);
        DbDataSourceImpl dbDataSource = (DbDataSourceImpl) dataSources.get(0);

        ImageView child = new ImageView(new Image(getClass().getResourceAsStream("/myToolWindow/plus.png")));
        TreeItem<String> root = new TreeItem<>("Tables", child);
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
                            root.getChildren().add(new TreeItem<>(myName, child));
//                            System.out.println(myName);
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            e.printStackTrace();
                        }

                    }
            );
            databaseView.setRoot(root);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    // loads some strings into the tree in the application UI.
    public void loadTreeItems(String... rootItems) {
        final Node rootIcon = new ImageView(new Image(getClass().getResourceAsStream("/myToolWindow/Calendar-icon.png")));
        ImageView child = new ImageView(new Image(getClass().getResourceAsStream("/myToolWindow/plus.png")));

        TreeItem<String> root = new TreeItem<>("Root Node", child);
        root.setExpanded(true);
        for (String itemString : rootItems) {
            root.getChildren().add(new TreeItem<>(itemString, child));
            root.getChildren().add(new TreeItem<>(itemString, child));
            root.getChildren().add(new TreeItem<>(itemString, child));
            root.getChildren().add(new TreeItem<>(itemString, child));
            root.getChildren().add(new TreeItem<>(itemString, child));
            root.getChildren().add(new TreeItem<>(itemString, child));
            root.getChildren().add(new TreeItem<>(itemString, child));
            root.getChildren().add(new TreeItem<>(itemString, child));
            root.getChildren().add(new TreeItem<>(itemString, child));
            root.getChildren().add(new TreeItem<>(itemString, child));
        }

        TreeItem<String> root2 = new TreeItem<>("Root Node3", child);
//       root2.setExpanded(true);
        for (String itemString : rootItems) {
            root2.getChildren().add(new TreeItem<>(itemString, child));
            root2.getChildren().add(new TreeItem<>(itemString, child));
        }
        root.getChildren().add(root2);

        tableColumn1.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getValue()));

        databaseView.setRoot(root);

    }

    class Person {
        public Person(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String name;
    }
}
