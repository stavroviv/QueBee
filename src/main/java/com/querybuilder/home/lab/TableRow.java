package com.querybuilder.home.lab;

import lombok.Data;

@Data
public class TableRow {
    private boolean nested;
    private String name;
    private String query;
    private boolean root;
    private String comboBoxValue;

    public TableRow(String name) {
        this(name, false);
    }

    public TableRow(String name, boolean root) {
        this.name = name;
        this.root = root;
    }

}
