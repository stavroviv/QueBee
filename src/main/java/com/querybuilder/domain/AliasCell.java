package com.querybuilder.domain;

import com.querybuilder.controllers.MainController;
import com.querybuilder.domain.qparts.OneCte;
import com.querybuilder.utils.Utils;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.List;

import static com.querybuilder.utils.Constants.EMPTY_UNION_VALUE;
import static com.querybuilder.utils.Utils.doubleClick;
import static com.querybuilder.utils.Utils.removeEmptyAliases;

public class AliasCell extends TableCell<AliasRow, String> {
    private MainController controller;
    private String columnName;

    public AliasCell(TableColumn<AliasRow, String> aliasColumn, String columnName, MainController controller) {
        this.controller = controller;
        this.columnName = columnName;

        aliasColumn.setOnEditCommit(t -> {
            int row = t.getTablePosition().getRow();
            AliasRow currentRow = t.getTableView().getItems().get(row);
            String[] newValue = t.getNewValue().split("_");
            currentRow.getValues().put(columnName, newValue[0]);
            currentRow.getIds().put(columnName, Long.parseLong(newValue[1]));

            removeEmptyAliases(controller);
        });
    }

    @Override
    public void startEdit() {
        if (isEmpty()) {
            return;
        }
        super.startEdit();

        setText(null);

        HBox pane = new HBox();
        pane.setMaxHeight(10);

        TextField textField = new TextField(getItem());
        textField.setEditable(false);
        textField.prefWidthProperty().bind(pane.widthProperty());
        if (getItem().equals(EMPTY_UNION_VALUE)) {
            textField.setText("");
        }
        pane.getChildren().add(textField);

        Button leftPart = new Button("...");
        leftPart.setAlignment(Pos.CENTER_RIGHT);
        leftPart.setOnMouseClicked(event -> showPopup(this));
        pane.getChildren().add(leftPart);

        Button clearButton = new Button("x");
        clearButton.setAlignment(Pos.CENTER_RIGHT);
        clearButton.setOnMouseClicked(event -> {
            commitEdit(EMPTY_UNION_VALUE);
            if (aliasPopup != null) {
                aliasPopup.hide();
            }
        });
        pane.getChildren().add(clearButton);

        setGraphic(pane);
        textField.selectAll();
    }

    private PopupControl aliasPopup;

    private void showPopup(TableCell<AliasRow, String> cell) {
        aliasPopup = new PopupControl();
        aliasPopup.setAutoHide(true);
        aliasPopup.setAutoFix(true);
        aliasPopup.setHideOnEscape(true);

        final Scene scene = cell.getScene();
        final Point2D windowCoord = new Point2D(scene.getWindow().getX(), scene.getWindow().getY());
        final Point2D sceneCoord = new Point2D(scene.getX(), scene.getY());
        final Point2D nodeCoord = cell.localToScene(0.0, 0.0);
        final double clickX = Math.round(windowCoord.getX() + sceneCoord.getX() + nodeCoord.getX());
        final double clickY = Math.round(windowCoord.getY() + sceneCoord.getY() + nodeCoord.getY());

        Utils.setDefaultSkin(aliasPopup, getAliasTable(), cell);
        aliasPopup.show(cell, clickX, clickY + cell.getHeight());
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setText(getItem());
        setGraphic(null);
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setText(null);
            setGraphic(null);
            return;
        }
        Label textField = new Label(item);
        this.setGraphic(textField);
    }

    private TableView<TableRow> getAliasTable() {
        TableView<TableRow> aliasTableContext = new TableView<>();
        aliasTableContext.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<TableRow, TableRow> aliasTableContextColumn = new TableColumn<>();
        aliasTableContext.getColumns().add(aliasTableContextColumn);
        aliasTableContextColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        List<TableRow> items = new ArrayList<>();

        TabPane unionTabPane = controller.getUnionTabPane();
        if (!unionTabPane.getTabs().isEmpty() && columnName.equals(unionTabPane.getSelectionModel().getSelectedItem().getId())) {
            controller.getTableFieldsController().getFieldTable().getItems().forEach(tableRow ->
                    items.add(TableRow.tableRowFromValue(tableRow))
            );
        } else {
            // get from query
            OneCte cte = controller.getCurrentCte();
            for (TableRow item : cte.getUnionMap().get(columnName).getFieldTable().getItems()) {
                items.add(TableRow.tableRowFromValue(item));
            }
        }

        aliasTableContext.getItems().addAll(items);
        aliasTableContext.getSelectionModel().select(0);

        Utils.setEmptyHeader(aliasTableContext);

        aliasTableContext.setOnMousePressed(e -> {
            if (!doubleClick(e)) {
                return;
            }
            TableRow selectedItem = aliasTableContext.getSelectionModel().getSelectedItem();

            ObservableList<AliasRow> aliasItems = controller.getUnionAliasesController().getAliasTable().getItems();
            for (AliasRow item : aliasItems) {
                Long aLong = item.getIds().get(columnName);
                if (aLong == null) {
                    continue;
                }
                if (selectedItem.getId() == aLong) {
                    item.getValues().put(columnName, EMPTY_UNION_VALUE);
                    item.getIds().remove(columnName);
                }
            }

            commitEdit(selectedItem.getName() + "_" + selectedItem.getId());
            aliasPopup.hide();

            controller.getUnionAliasesController().getAliasTable().refresh();
        });
        return aliasTableContext;
    }

}
