package com.querybuilder.home.lab;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class MainController {

    @FXML
    private TreeView<String> locationTreeView;

    // the initialize method is automatically invoked by the FXMLLoader - it's magic
    public void initialize() {
        loadTreeItems("initial 1", "initial 2", "initial 3");
    }

    // loads some strings into the tree in the application UI.
    public void loadTreeItems(String... rootItems) {
//        final Node rootIcon =  new ImageView(new Image(getClass().getResourceAsStream("/myToolWindow/Calendar-icon.png")));
//        ImageView child =  new ImageView(new Image(getClass().getResourceAsStream("/myToolWindow/plus.png")));
//
//        TreeItem<String> root = new TreeItem<>("Root Node", child);
//        root.setExpanded(true);
//        for (String itemString: rootItems) {
//            root.getChildren().add(new TreeItem<>(itemString, child));
//            root.getChildren().add(new TreeItem<>(itemString, child));
//            root.getChildren().add(new TreeItem<>(itemString, child));
//            root.getChildren().add(new TreeItem<>(itemString, child));
//            root.getChildren().add(new TreeItem<>(itemString, child));
//            root.getChildren().add(new TreeItem<>(itemString, child));
//            root.getChildren().add(new TreeItem<>(itemString, child));
//            root.getChildren().add(new TreeItem<>(itemString, child));
//            root.getChildren().add(new TreeItem<>(itemString, child));
//            root.getChildren().add(new TreeItem<>(itemString, child));
//        }
//
//        TreeItem<String> root2 = new TreeItem<>("Root Node3", child);
////       root2.setExpanded(true);
//        for (String itemString: rootItems) {
//            root2.getChildren().add(new TreeItem<>(itemString, child));
//            root2.getChildren().add(new TreeItem<>(itemString, child));
//        }
//        root.getChildren().add(root2);
//        locationTreeView.setRoot(root);
    }
}
