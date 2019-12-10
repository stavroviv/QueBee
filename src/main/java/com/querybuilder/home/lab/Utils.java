package com.querybuilder.home.lab;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

class Utils {

    static void setEmptyHeader(Control control) {
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

    static void setDefaultSkin(PopupControl popup, Control control, Control cell) {
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

    static void setCellFactory(TreeTableColumn<TableRow, TableRow> tablesViewColumn) {
        tablesViewColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getValue()));
        tablesViewColumn.setCellFactory(ttc -> new TreeTableCell<TableRow, TableRow>() {
            @Override
            protected void updateItem(TableRow item, boolean empty) {
                super.updateItem(item, empty);
                Utils.setItem(this, item, empty);
            }
        });
    }

    static void setItem(TreeTableCell<TableRow, TableRow> cell, TableRow item, boolean empty) {
        ImageView element = new ImageView(new Image(Utils.class.getResourceAsStream("/myToolWindow/element.png")));
        ImageView table = new ImageView(new Image(Utils.class.getResourceAsStream("/myToolWindow/table.png")));
        ImageView nestedQuery = new ImageView(new Image(Utils.class.getResourceAsStream("/myToolWindow/nestedQuery.png")));
        cell.setText(empty ? null : item.getName());
        // icons
        if (empty) {
            cell.setGraphic(null);
        } else if (item.isNested()) {
            cell.setGraphic(nestedQuery);
        } else {
            cell.setGraphic(item.isRoot() ? table : element);
        }
    }

}