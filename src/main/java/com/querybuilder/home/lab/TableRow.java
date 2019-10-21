package com.querybuilder.home.lab;

import javafx.beans.property.SimpleStringProperty;
import lombok.Data;

@Data
public class TableRow {
    private boolean nested;
    private String name;
    private String query;
    private boolean root;
    private boolean notSelectable;

    private SimpleStringProperty comboBoxValue = new SimpleStringProperty();

    public TableRow(String name) {
        this(name, false);
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
