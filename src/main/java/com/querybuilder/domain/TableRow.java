package com.querybuilder.domain;

import javafx.beans.property.SimpleStringProperty;
import lombok.Data;

@Data
public class TableRow {
    private static long lastId;

    private long id;
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
        this.name = name;
        this.id = ++lastId;
    }

    private TableRow() {
    }

    public static TableRow tableRowFromValue(TableRow value) {
        TableRow row = new TableRow();
        row.name = value.getName();
        row.id = value.getId();
        row.root = value.isRoot();
        row.cte = value.isCte();
        return row;
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
