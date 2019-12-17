package com.querybuilder.home.lab.domain;

import javafx.beans.property.SimpleStringProperty;
import lombok.Data;

@Data
public class TableRow {
    private boolean nested;
    private String name;
    private String alias;
    private String query;
    private boolean distinct;
    private boolean root;
    private boolean notSelectable;

    private SimpleStringProperty comboBoxValue = new SimpleStringProperty();

    public TableRow(String name) {
        this(name, false);
    }

    public TableRow(String name, String alias) {
        this(name, false);
        this.alias = alias;
    }

    public TableRow(String name, boolean root) {
        this.name = name;
        this.root = root;
    }

    public String getComboBoxValue() {
        return comboBoxValue.get();
    }

    public void setComboBoxValue(String comboBoxValue) {
        this.comboBoxValue.set(comboBoxValue);
    }

    public SimpleStringProperty comboBoxValueProperty() {
        return comboBoxValue;
    }

}
