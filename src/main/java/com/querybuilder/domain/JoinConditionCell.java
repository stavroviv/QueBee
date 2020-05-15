package com.querybuilder.domain;

import com.querybuilder.controllers.MainController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import static com.querybuilder.utils.Constants.EXPRESSIONS;

public class JoinConditionCell extends TableCell<LinkElement, LinkElement> {
    private MainController controller;
    private final static ObservableList<String> comparison = FXCollections.observableArrayList("=", "<>", "<", ">", "<=", ">=");

    public JoinConditionCell(MainController controller) {
        this.controller = controller;
    }

    @Override
    protected void updateItem(LinkElement item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
            return;
        }

        if (item.isCustom()) {
            TextField customCondition = new TextField();
            customCondition.setText(item.getCondition());
            customCondition.textProperty().addListener((observable, oldValue, newValue) -> {
                item.setCondition(newValue);
            });
            setGraphic(customCondition);
            return;
        }

        ComboBox<String> comparisonComboBox = new ComboBox<>(comparison);
        comparisonComboBox.valueProperty().addListener(
                (observable, oldValue, newValue) -> item.setExpression(newValue)
        );
        item.getConditionComboBox1().valueProperty().addListener(
                (observable, oldValue, newValue) -> item.setField1(newValue)
        );
        item.getConditionComboBox2().valueProperty().addListener(
                (observable, oldValue, newValue) -> item.setField2(newValue)
        );

        HBox pane = new HBox();
        item.getConditionComboBox1().prefWidthProperty().bind(pane.widthProperty());
        comparisonComboBox.setMinWidth(70);
        item.getConditionComboBox2().prefWidthProperty().bind(pane.widthProperty());

        String condition = item.getCondition();
        if (condition != null) {
            String[] array = condition.split(EXPRESSIONS);
            item.getConditionComboBox1().setValue(array[0]);
            comparisonComboBox.setValue(condition.replace(array[0], "").replace(array[1], ""));
            item.getConditionComboBox2().setValue(array[1]);
        }

        pane.getChildren().add(item.getConditionComboBox1());
        pane.getChildren().add(comparisonComboBox);
        pane.getChildren().add(item.getConditionComboBox2());

        setGraphic(pane);
    }
}
