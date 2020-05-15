package com.querybuilder.domain;

import com.querybuilder.controllers.MainController;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class LinkElement {
    private MainController mainController;

    private SimpleStringProperty table1 = new SimpleStringProperty();
    private SimpleStringProperty table2 = new SimpleStringProperty();
    private BooleanProperty allTable1 = new SimpleBooleanProperty();
    private BooleanProperty allTable2 = new SimpleBooleanProperty();
    private BooleanProperty custom = new SimpleBooleanProperty();
    private String condition;
    private ComboBox<String> conditionComboBox1 = new ComboBox<>();
    private ComboBox<String> conditionComboBox2 = new ComboBox<>();

    private String field1;
    private String expression;
    private String field2;

    public LinkElement(MainController mainController, String table1, String table2, boolean allTable1, boolean allTable2, boolean custom) {
        this.mainController = mainController;
        setTablesCombobox();

        setTable1(table1);
        setTable2(table2);
        setAllTable1(allTable1);
        setAllTable2(allTable2);
        setCustom(custom);
    }

    private void setTablesCombobox() {
        List<String> tables = new ArrayList<>();
        mainController.getTablesView().getRoot().getChildren().forEach(
                x -> tables.add(x.getValue().getName())
        );
        ObservableList<String> items = FXCollections.observableArrayList(tables);
        table1ComboBox.setItems(items);
        table2ComboBox.setItems(items);
    }

    public LinkElement(MainController mainController) {
        this.mainController = mainController;
        setTablesCombobox();
        setTable1("");
        setTable2("");
        setAllTable1(false);
        setAllTable2(false);
        setCustom(false);
        setExpression("=");
    }

    private ComboBox<String> table1ComboBox = new ComboBox<>();
    private ComboBox<String> table2ComboBox = new ComboBox<>();

    public ComboBox<String> getTable1ComboBox() {
        return table1ComboBox;
    }

    public void setTable1ComboBox(ComboBox<String> table1ComboBox) {
        this.table1ComboBox = table1ComboBox;
    }

    public ComboBox<String> getTable2ComboBox() {
        return table2ComboBox;
    }

    public void setTable2ComboBox(ComboBox<String> table2ComboBox) {
        this.table2ComboBox = table2ComboBox;
    }

    public ComboBox<String> getConditionComboBox1() {
        return conditionComboBox1;
    }

    public void setConditionComboBox1(ComboBox<String> conditionComboBox1) {
        this.conditionComboBox1 = conditionComboBox1;
    }

    public ComboBox<String> getConditionComboBox2() {
        return conditionComboBox2;
    }

    public void setConditionComboBox2(ComboBox<String> conditionComboBox2) {
        this.conditionComboBox2 = conditionComboBox2;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public Boolean isCustom() {
        return custom.get();
    }

    public BooleanProperty customProperty() {
        return custom;
    }

    public void setCustom(Boolean custom) {
        this.custom.set(custom);
    }

    public boolean isAllTable1() {
        return allTable1.get();
    }

    public void setAllTable1(boolean allTable1) {
        this.allTable1.set(allTable1);
    }

    public BooleanProperty allTable1Property() {
        return allTable1;
    }

    public boolean isAllTable2() {
        return allTable2.get();
    }

    public void setAllTable2(boolean allTable2) {
        this.allTable2.set(allTable2);
    }

    public BooleanProperty allTable2Property() {
        return allTable2;
    }

    public String getTable1() {
        return table1.get();
    }

    public void setTable1(String table1) {
        this.table1.addListener((observable, oldValue, newValue) -> {
            ObservableList<String> columns = FXCollections.observableArrayList(
                    mainController.getDbElements().get(newValue)
            );
            conditionComboBox1.setItems(columns);
        });
        this.table1.set(table1);
    }

    public SimpleStringProperty table1Property() {
        return table1;
    }

    public String getTable2() {
        return table2.get();
    }

    public void setTable2(String table2) {
        this.table2.addListener((observable, oldValue, newValue) -> {
            ObservableList<String> columns = FXCollections.observableArrayList(
                    mainController.getDbElements().get(newValue)
            );
            conditionComboBox2.setItems(columns);
        });
        this.table2.set(table2);
    }

    public SimpleStringProperty table2Property() {
        return table2;
    }

    public LinkElement clone() {
        return new LinkElement(mainController, getTable1(), getTable2(), isAllTable1(), isAllTable2(), isCustom());
    }
}
