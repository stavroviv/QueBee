package com.querybuilder.domain;

import com.querybuilder.controllers.MainController;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.layout.AnchorPane;

public class LinkTableCell extends TableCell<LinkElement, LinkElement> {
    private MainController controller;
    private String tableName;

    public LinkTableCell(MainController controller, String tableName) {
        this.controller = controller;
        this.tableName = tableName;
    }

    @Override
    protected void updateItem(LinkElement item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
            return;
        }

        ComboBox<String> box = new ComboBox<>();

        if ("table1".equals(tableName)) {
            box.setItems(item.getTable1ComboBox().getItems());
            box.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == null) {
                    return;
                }
                item.setTable1(newValue);
//                item.getTable2ComboBox().getItems().remove(newValue);
            });
            box.setValue(item.getTable1());
        } else if ("table2".equals(tableName)) {
            box.setItems(item.getTable2ComboBox().getItems());
            box.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == null) {
                    return;
                }
                item.setTable2(newValue);
//                item.getTable1ComboBox().getItems().remove(newValue);
            });
            box.setValue(item.getTable2());
        }

        AnchorPane.setRightAnchor(box, 0.0d);
        AnchorPane.setLeftAnchor(box, 0.0d);

        AnchorPane pane = new AnchorPane();
        pane.getChildren().add(box);
        setGraphic(pane);
    }
}
