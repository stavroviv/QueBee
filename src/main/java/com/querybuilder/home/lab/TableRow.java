package com.querybuilder.home.lab;

public class TableRow {
    private boolean nested;
    private String name;
    private String query;
    private boolean root;

    public boolean isRoot() {
        return root;
    }

    public void setRoot(boolean root) {
        this.root = root;
    }


    public TableRow(String name) {
        this(name, false);
    }

    public TableRow(String name, boolean root) {
        this.name = name;
        this.root = root;
    }

    public boolean isNested() {
        return nested;
    }

    public void setNested(boolean nested) {
        this.nested = nested;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
