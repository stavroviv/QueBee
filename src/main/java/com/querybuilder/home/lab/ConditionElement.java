package com.querybuilder.home.lab;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.ComboBox;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class ConditionElement extends TableRow {
    private BooleanProperty custom = new SimpleBooleanProperty();
    private String condition;
    private ComboBox<String> conditionComboBox1;

    private String leftExpression;
    private String expression;
    private String rightExpression;

    public ConditionElement(String name) {
        super(name);
        condition = name;
    }

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
