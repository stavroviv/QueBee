package com.querybuilder.home.lab;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.util.List;

import static com.querybuilder.home.lab.Utils.setDefaultSkin;
import static com.querybuilder.home.lab.Utils.setEmptyHeader;

public class AliasCell extends TableCell<AliasRow, String> {
    private TableColumn<AliasRow, String> aliasRow;
    private List<String> items;

    public AliasCell(TableColumn<AliasRow, String> aliasRow, int column, List<String> items) {
        this.aliasRow = aliasRow;
        this.items = items;
        initConditionTableForPopup();
        aliasRow.setOnEditCommit(
                t -> {
                    int row = t.getTablePosition().getRow();
                    AliasRow currentRow = t.getTableView().getItems().get(row);
                    currentRow.getValues().set(column, t.getNewValue());
                }
        );
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
        textField.prefWidthProperty().bind(pane.widthProperty());

        pane.getChildren().add(textField);

        Button leftPart = new Button("...");
        leftPart.setAlignment(Pos.CENTER_RIGHT);
        leftPart.setOnMouseClicked(event -> showPopup(this));
        pane.getChildren().add(leftPart);

        Button clearButton = new Button("x");
        clearButton.setAlignment(Pos.CENTER_RIGHT);
//        clearButton.setOnMouseClicked(event -> showPopup(event, this));
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
        setDefaultSkin(aliasPopup, aliasTableContext, cell);
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
        } else {
            pppp(item, empty, this);

        }
    }

    private void pppp(String item, boolean empty, TableCell<AliasRow, String> aliasRowStringTableCell) {
        if (aliasRowStringTableCell.isEditing()) {
            TextField textField = new TextField(empty ? null : item);
            aliasRowStringTableCell.setGraphic(textField);
            textField.setOnMouseClicked(event -> {
                System.out.println(event);
            });
        } else {
            Label textField = new Label(empty ? null : item);
            aliasRowStringTableCell.setGraphic(textField);
        }
    }

    private TableView<String> aliasTableContext;
    private TableColumn<String, String> aliasTableContextColumn;

    private void initConditionTableForPopup() {
        aliasTableContext = new TableView<>();
        aliasTableContext.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        aliasTableContextColumn = new TableColumn<>();
        aliasTableContext.getColumns().add(aliasTableContextColumn);
        aliasTableContextColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()));

        aliasTableContext.getItems().addAll(items);
        aliasTableContext.getSelectionModel().select(0);

        setEmptyHeader(aliasTableContext);

        aliasTableContext.setOnMousePressed(e -> {
            if (e.getClickCount() == 2 && e.isPrimaryButtonDown()) {
                String selectedItem = aliasTableContext.getSelectionModel().getSelectedItem();
                commitEdit(selectedItem);
                aliasPopup.hide();
            }
        });
    }
}
