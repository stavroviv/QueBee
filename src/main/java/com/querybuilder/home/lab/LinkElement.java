package com.querybuilder.home.lab;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

public class LinkElement {
    private SimpleStringProperty table1 = new SimpleStringProperty();
    private SimpleStringProperty table2 = new SimpleStringProperty();
    private BooleanProperty allTable1 = new SimpleBooleanProperty();
    private BooleanProperty allTable2 = new SimpleBooleanProperty();
    private BooleanProperty linkTableCustom = new SimpleBooleanProperty();

    public LinkElement(String table1, String table2, boolean allTable1, boolean allTable2) {
        setTable1(table1);
        setTable2(table2);
        setAllTable1(allTable1);
        setAllTable2(allTable2);
        setLinkTableCustom(false);
    }

    public boolean isLinkTableCustom() {
        return linkTableCustom.get();
    }

    public BooleanProperty linkTableCustomProperty() {
        return linkTableCustom;
    }

    public void setLinkTableCustom(boolean linkTableCustom) {
        this.linkTableCustom.set(linkTableCustom);
    }

    public boolean isAllTable1() {
        return allTable1.get();
    }

    public BooleanProperty allTable1Property() {
        return allTable1;
    }

    public void setAllTable1(boolean allTable1) {
        this.allTable1.set(allTable1);
    }

    public boolean isAllTable2() {
        return allTable2.get();
    }

    public BooleanProperty allTable2Property() {
        return allTable2;
    }

    public void setAllTable2(boolean allTable2) {
        this.allTable2.set(allTable2);
    }

    public String getTable1() {
        return table1.get();
    }

    public SimpleStringProperty table1Property() {
        return table1;
    }

    public void setTable1(String table1) {
        this.table1.set(table1);
    }

    public String getTable2() {
        return table2.get();
    }

    public SimpleStringProperty table2Property() {
        return table2;
    }

    public void setTable2(String table2) {
        this.table2.set(table2);
    }


}
