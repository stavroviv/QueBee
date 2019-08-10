package com.querybuilder.home.lab;

import com.intellij.database.model.DasModel;
import com.intellij.database.model.DasObject;
import com.intellij.database.model.ObjectKind;
import com.intellij.database.psi.DbDataSource;
import com.intellij.database.util.DasUtil;
import com.intellij.database.util.DbUtil;
import com.intellij.database.view.DatabaseStructure;
import com.intellij.database.view.DatabaseView;
import com.intellij.facet.FacetConfiguration;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.containers.JBTreeTraverser;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.Set;

//import com.intellij.modules.*;

public class MainController {
    @FXML
    private TreeView<String> locationTreeView;
    @FXML
    private Button okButton;
    @FXML
    private TreeTableView<String> databaseView;

    // the initialize method is automatically invoked by the FXMLLoader - it's magic
    public void initialize() {
        loadTreeItems("initial 1", "initial 2", "initial 3");
    }

    @FXML
    public void onClickMethod() {
//        com.intellij.modules.idea.ultimate
//        com.intellij.sql.database.SqlDataSource;
        Project p = ProjectManager.getInstance().getOpenProjects()[0];
        Application application = ApplicationManager.getApplication();


//        Platform.runLater(() -> {
//            try {
//
//            } catch (Exception e) {
//                System.out.println(e);
//            }
//
//        });
        Set<String> existingDataSourceNames = DbUtil.getExistingDataSourceNames(p);
        JBIterable<DbDataSource> dataSources = DbUtil.getDataSources(p);
        DbDataSource dbDataSource = dataSources.get(0);

        DasObject schemaObject = DasUtil.getSchemaObject(dbDataSource);


        DasObject dasObject = DbUtil.getDasObject(dbDataSource);
        ObjectKind kind = dasObject.getKind();
        JBIterable<? extends DasObject> dasChildren = dasObject.getDasChildren(kind);
        for (DasObject dasChild : dasChildren) {
            System.out.println(dasChild);
        }
//        dbDataSource.getFirstChild()

//        DasUtil.
        PsiElement firstChild = dbDataSource.getFirstChild();
        System.out.println(firstChild);


//        JBIterable<? extends DasObject> modelRoots = dbDataSource.getModel().getModelRoots();
//        modelRoots.forEach(x-> System.out.println(x));
////        DasUtil.dasTraverser()
////        FacetConfiguration().
//
//
//
//        System.out.println(dasObject);
//        DatabaseView view = DatabaseView.getDatabaseView(p);
////        Class<DasModel> tClass = new DasModel();
////        DasModel model = DatabaseStructure.getModel(dataSources.get(0));
////        DbImplUtil.
//        view.setVisible(true);
//        System.out.println(p);
////        dasObject.
//        System.out.println(existingDataSourceNames);
//        okButton.setText("Thanks!");
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
        databaseView.setRoot(root);
    }
}
