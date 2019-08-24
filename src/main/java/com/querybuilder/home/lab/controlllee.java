package com.querybuilder.home.lab;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class controlllee {
    @FXML
    private TableView<String> fieldTable;
    @FXML
    private TableColumn<String, String> fieldColumn;
    @FXML
    public void addFieldRowAction() {
        fieldTable.getItems().add("test");
    }

    @FXML
    public void deleteFIeldRow() {
        String selectedItem = fieldTable.getSelectionModel().getSelectedItem();
        fieldTable.getItems().remove(selectedItem);
    }

    public void initialize() {
//        fillDatabaseTables();
        fieldColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()));
//        selectItems.forEach(x -> fieldTable.getItems().add(x.toString()));


    }
}
