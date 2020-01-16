package com.querybuilder.home.lab.utils;

import com.querybuilder.home.lab.controllers.Argumentative;
import com.querybuilder.home.lab.domain.TableRow;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Map;

public class Utils {

    public static void setEmptyHeader(Control control) {
        control.widthProperty().addListener((ov, t, t1) -> {
            Pane header = (Pane) control.lookup("TableHeaderRow");
            if (header != null && header.isVisible()) {
                header.setMaxHeight(0);
                header.setMinHeight(0);
                header.setPrefHeight(0);
                header.setVisible(false);
                header.setManaged(false);
            }
        });
    }

    public static void setDefaultSkin(PopupControl popup, Control control, Control cell) {
        popup.setSkin(new Skin<Skinnable>() {
            @Override
            public Skinnable getSkinnable() {
                return null;
            }

            @Override
            public Node getNode() {
                control.setMinWidth(cell.getWidth());
                control.setMaxWidth(cell.getWidth());
                return control;
            }

            @Override
            public void dispose() {
            }
        });
    }

    public static void setCellFactory(TreeTableColumn<TableRow, TableRow> tablesViewColumn) {
        tablesViewColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getValue()));
        tablesViewColumn.setCellFactory(ttc -> new CustomCell());
    }

    public static void openForm(String formName, String title, Map<String, Object> userData) {
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(getScene(formName, userData));
        stage.setOnCloseRequest((e) -> {
            System.out.println("sdfsdf");
        });
        stage.show();
    }

    public static Scene getScene(String formName, Map<String, Object> userData) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Utils.class.getResource(formName));
            FXMLLoader.setDefaultClassLoader(Utils.class.getClassLoader());
            Parent root = fxmlLoader.load();
            Argumentative controller = fxmlLoader.getController();
            controller.initData(userData);
            return new Scene(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean doubleClick(MouseEvent e) {
        return e.getClickCount() == 2 && e.isPrimaryButtonDown();
    }
}
