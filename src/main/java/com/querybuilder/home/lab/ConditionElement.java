package com.querybuilder.home.lab;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.ComboBox;

public class ConditionElement extends TableRow {
    private BooleanProperty custom = new SimpleBooleanProperty();
    private String condition;
    private ComboBox<String> conditionComboBox1;

    public ConditionElement(String name) {
        super(name);
        condition = name;
    }

//    public ConditionElement(String table1, String table2, boolean allTable1, boolean allTable2, boolean custom, Map<String, List<String>> dbElements) {
//        setTable1(table1);
//        setTable2(table2);
//        setAllTable1(allTable1);
//        setAllTable2(allTable2);
//        setCustom(custom);
//        setConditionComboBox1(new ComboBox<>());
//        setConditionComboBox2(new ComboBox<>());
//        this.dbElements = dbElements;
//    }

    public ComboBox<String> getConditionComboBox1() {
        return conditionComboBox1;
    }

    public void setConditionComboBox1(ComboBox<String> conditionComboBox1) {
        this.conditionComboBox1 = conditionComboBox1;
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

}
