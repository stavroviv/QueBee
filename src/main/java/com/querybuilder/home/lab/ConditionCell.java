package com.querybuilder.home.lab;

import com.sun.javafx.scene.control.skin.LabeledText;
import javafx.collections.FXCollections;
import javafx.event.EventTarget;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import static com.querybuilder.home.lab.Constants.DATABASE_ROOT;
import static com.querybuilder.home.lab.Utils.*;
import static javafx.scene.control.TreeTableView.CONSTRAINED_RESIZE_POLICY;

public class ConditionCell extends TableCell<ConditionElement, ConditionElement> {
    private final TextField customCondition = new TextField();
    private final ComboBox<String> comparisonComboBox = new ComboBox<>(
            FXCollections.observableArrayList("=", "<>", "<", ">", "<=", ">=")
    );

    TreeTableView<TableRow> tablesView;
    TableView<ConditionElement> conditionTableResults;

    public ConditionCell(TableView<ConditionElement> conditionTableResults, TreeTableView<TableRow> tablesView) {
        this.tablesView = tablesView;
        this.conditionTableResults = conditionTableResults;
        initConditionTableForPopup();
    }

    @Override
    protected void updateItem(ConditionElement item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
        } else if (item.isCustom()) {
            if (item.getCondition().isEmpty()) {
                String cond = item.getLeftExpression() + item.getExpression() + item.getRightExpression();
                item.setCondition(cond);
                item.setLeftExpression("");
                item.setExpression("");
                item.setRightExpression("");
            }
            customCondition.setText(item.getCondition());
            customCondition.textProperty().addListener(
                    (observable, oldValue, newValue) -> {
                        item.setCondition(newValue);
                    });
            setGraphic(customCondition);
        } else {
            HBox pane = new HBox();
            String condition1 = item.getCondition();
            if (!condition1.isEmpty()) {
                String[] array = condition1.split("[>=<=<>]+");
                String leftExpresion = condition1;
                String expression = "=";
                String rightExpression = "?";
                if (array.length == 2) {
                    leftExpresion = array[0];
                    expression = condition1.replace(array[0], "").replace(array[1], "");
                    comparisonComboBox.setValue(expression);
                    rightExpression = array[1];
                }
                item.setLeftExpression(leftExpresion);
                item.setExpression(expression);
                item.setRightExpression(rightExpression);
                item.setCondition("");
            }

            Button leftPart = new Button(item.getLeftExpression());
            leftPart.setMnemonicParsing(false);
            leftPart.setAlignment(Pos.CENTER_LEFT);
            leftPart.prefWidthProperty().bind(pane.widthProperty());
            leftPart.setOnMouseClicked(event -> showPopup(event, this, item));
            pane.getChildren().add(leftPart);

            comparisonComboBox.setMinWidth(70);
            comparisonComboBox.setValue(item.getExpression());
            comparisonComboBox.valueProperty().addListener(
                    (observable, oldValue, newValue) -> item.setExpression(newValue)
            );
            pane.getChildren().add(comparisonComboBox);

            TextField rightPart = new TextField(item.getRightExpression());
            rightPart.textProperty().addListener(
                    (observable, oldValue, newValue) -> item.setRightExpression(newValue)
            );
            rightPart.prefWidthProperty().bind(pane.widthProperty());
            pane.getChildren().add(rightPart);

            setGraphic(pane);
        }
    }

    private PopupControl conditionPopup;
    private TreeTableView<TableRow> conditionsTreeTableContext;
    private TreeTableColumn<TableRow, TableRow> conditionsTreeTableContextColumn;
    private SelectedFieldsTree selectedConditionsTreeTableContext;

    private void initConditionTableForPopup() {
        conditionsTreeTableContext = new TreeTableView<>();
        conditionsTreeTableContext.setShowRoot(false);
        conditionsTreeTableContext.setColumnResizePolicy(CONSTRAINED_RESIZE_POLICY);

        conditionsTreeTableContextColumn = new TreeTableColumn<>();
        conditionsTreeTableContext.getColumns().add(conditionsTreeTableContextColumn);
        setCellFactory(conditionsTreeTableContextColumn);

        selectedConditionsTreeTableContext = new SelectedFieldsTree(tablesView);
        conditionsTreeTableContext.setRoot(selectedConditionsTreeTableContext);
        setEmptyHeader(conditionsTreeTableContext);
        conditionsTreeTableContext.setOnMousePressed(e -> {
            if (e.getClickCount() == 2 && e.isPrimaryButtonDown()) {
                TreeItem<TableRow> item = conditionsTreeTableContext.getSelectionModel().getSelectedItem();
                String parentName = item.getParent().getValue().getName();
                if (DATABASE_ROOT.equals(parentName)) {
                    return;
                }
                ConditionElement conditionElement = conditionTableResults.getSelectionModel().getSelectedItem();
                String name = parentName + "." + item.getValue().getName();
                conditionElement.setLeftExpression(name);
                conditionPopup.hide();
                conditionTableResults.refresh();
            }
        });
    }

//    void setCellFactory(TreeTableColumn<TableRow, TableRow> tablesViewColumn) {
//        tablesViewColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getValue()));
//        tablesViewColumn.setCellFactory(ttc -> new TreeTableCell<TableRow, TableRow>() {
//            private final ImageView element = new ImageView(new Image(getClass().getResourceAsStream("/myToolWindow/element.png")));
//            private final ImageView table = new ImageView(new Image(getClass().getResourceAsStream("/myToolWindow/table.png")));
//            private final ImageView nestedQuery = new ImageView(new Image(getClass().getResourceAsStream("/myToolWindow/nestedQuery.png")));
//
//            @Override
//            protected void updateItem(TableRow item, boolean empty) {
//                super.updateItem(item, empty);
//                setText(empty ? null : item.getName());
//                // icons
//                if (empty) {
//                    setGraphic(null);
//                } else if (item.isNested()) {
//                    setGraphic(nestedQuery);
//                } else {
//                    setGraphic(item.isRoot() ? table : element);
//                }
//            }
//        });
//    }

    private void showPopup(MouseEvent event, TableCell<ConditionElement, ConditionElement> cell, ConditionElement item) {
        conditionPopup = new PopupControl();
        conditionPopup.setAutoHide(true);
        conditionPopup.setAutoFix(true);
        conditionPopup.setHideOnEscape(true);

        EventTarget target = event.getTarget();
        final Button targetButton;
        if (target instanceof Button) {
            targetButton = (Button) target;
        } else {
            LabeledText label = (LabeledText) target;
            targetButton = (Button) label.getParent();
        }
        final Scene scene = targetButton.getScene();
        final Point2D windowCoord = new Point2D(scene.getWindow().getX(), scene.getWindow().getY());
        final Point2D sceneCoord = new Point2D(scene.getX(), scene.getY());
        final Point2D nodeCoord = targetButton.localToScene(0.0, 0.0);
        final double clickX = Math.round(windowCoord.getX() + sceneCoord.getX() + nodeCoord.getX());
        final double clickY = Math.round(windowCoord.getY() + sceneCoord.getY() + nodeCoord.getY());

        setDefaultSkin(conditionPopup, conditionsTreeTableContext, targetButton);

        conditionPopup.show(cell, clickX, clickY + targetButton.getHeight());
        conditionTableResults.getSelectionModel().select(item);
    }
}
