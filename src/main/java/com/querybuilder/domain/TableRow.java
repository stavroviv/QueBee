package com.querybuilder.domain;

import javafx.beans.property.SimpleStringProperty;
import lombok.Data;

@Data
public class TableRow {
    private int id;
    private String name;
    private String alias;
    private String query;
    private boolean distinct;
    private boolean root;
    private boolean notSelectable;
    private boolean nested;
    private boolean cte;
    private boolean cteRoot;

    private SimpleStringProperty comboBoxValue = new SimpleStringProperty();

    public TableRow(String name) {
        this(name, -1, false);
    }

    public TableRow(String name, String alias) {
        this(name, -1, false);
        this.alias = alias;
    }

    public TableRow(String name, int id, boolean root) {
        this.name = name;
        this.id = id;
        this.root = root;
    }

    public TableRow(String name, int id, boolean root, boolean cte) {
        this.name = name;
        this.id = id;
        this.root = root;
        this.cte = cte;
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
