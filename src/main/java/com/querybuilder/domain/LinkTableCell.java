package com.querybuilder.domain;

import com.querybuilder.controllers.MainController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.layout.AnchorPane;

import java.util.ArrayList;
import java.util.List;

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

        List<String> tables = new ArrayList<>();
        ComboBox<String> box = new ComboBox<>();
        controller.getTablesView().getRoot().getChildren().forEach(
                x -> tables.add(x.getValue().getName())
        );

        ObservableList<String> items = FXCollections.observableArrayList(tables);
        box.setItems(items);
        if ("table1".equals(tableName)) {
            box.setValue(item.getTable1());
            box.valueProperty().addListener(
                    (observable, oldValue, newValue) -> item.setTable1(newValue)
            );
        } else if ("table2".equals(tableName)) {
            box.setValue(item.getTable2());
            box.valueProperty().addListener(
                    (observable, oldValue, newValue) -> item.setTable2(newValue)
            );
        }
        AnchorPane.setRightAnchor(box, 0.0d);
        AnchorPane.setLeftAnchor(box, 0.0d);

        AnchorPane pane = new AnchorPane();
        pane.getChildren().add(box);
        setGraphic(pane);
    }
}
