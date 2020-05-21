package com.querybuilder.domain;

import com.querybuilder.eventbus.CustomEvent;
import com.querybuilder.eventbus.CustomEventBus;
import com.querybuilder.eventbus.Subscriber;
import com.sun.javafx.scene.control.skin.LabeledText;
import javafx.collections.FXCollections;
import javafx.event.EventTarget;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import net.engio.mbassy.listener.Handler;

import java.util.Map;

import static com.querybuilder.utils.Constants.DATABASE_TABLE_ROOT;
import static com.querybuilder.utils.Constants.EXPRESSIONS;
import static com.querybuilder.utils.Utils.*;
import static javafx.scene.control.TreeTableView.CONSTRAINED_RESIZE_POLICY;

public class ConditionCell extends TableCell<ConditionElement, ConditionElement> implements Subscriber {
    public static final String REFRESH_SELECTED_TREE = "RefreshSelectedTree";
    private final TextField customCondition = new TextField();
    private final ComboBox<String> comparisonComboBox = new ComboBox<>(
            FXCollections.observableArrayList("=", "<>", "<", ">", "<=", ">=")
    );

    TreeTableView<com.querybuilder.domain.TableRow> tablesView;
    TableView<ConditionElement> conditionTableResults;

    public ConditionCell(TableView<ConditionElement> conditionTableResults, TreeTableView<com.querybuilder.domain.TableRow> tablesView) {
        this.tablesView = tablesView;
        this.conditionTableResults = conditionTableResults;
        initConditionTableForPopup();
        CustomEventBus.register(this);
    }

    @Override
    protected void updateItem(ConditionElement item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
            return;
        }

        if (item.isCustom()) {
            if (item.getCondition().isEmpty()) {
                String cond = item.getLeftExpression() + item.getExpression() + item.getRightExpression();
                item.setCondition(cond);
                item.setLeftExpression("");
                item.setExpression("");
                item.setRightExpression("");
            }
            customCondition.setText(item.getCondition());
            customCondition.textProperty().addListener((observable, oldValue, newValue) -> {
                item.setCondition(newValue);
            });
            setGraphic(customCondition);
            return;
        }

        String condition1 = item.getCondition();
        if (!condition1.isEmpty()) {
            String[] array = condition1.split(EXPRESSIONS);
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

        HBox pane = new HBox();
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

    private PopupControl conditionPopup;
    private TreeTableView<TableRow> conditionsTreeTableContext;
    private TreeTableColumn<TableRow, TableRow> conditionsTreeTableContextColumn; // ??????
    private SelectedFieldsTree selectedConditionsTreeTableContext;

    private void initConditionTableForPopup() {
        conditionsTreeTableContext = new TreeTableView<>();
        conditionsTreeTableContext.setShowRoot(false);
        conditionsTreeTableContext.setColumnResizePolicy(CONSTRAINED_RESIZE_POLICY);

        conditionsTreeTableContextColumn = new TreeTableColumn<>();
        conditionsTreeTableContext.getColumns().add(conditionsTreeTableContextColumn);
        setCellFactory(conditionsTreeTableContextColumn);

        selectedConditionsTreeTableContext = new SelectedFieldsTree(tablesView, conditionsTreeTableContext);
        setEmptyHeader(conditionsTreeTableContext);
        conditionsTreeTableContext.setOnMousePressed(e -> {
            if (!doubleClick(e)) {
                return;
            }
            TreeItem<TableRow> item = conditionsTreeTableContext.getSelectionModel().getSelectedItem();
            String parentName = item.getParent().getValue().getName();
            if (DATABASE_TABLE_ROOT.equals(parentName)) {
                return;
            }
            ConditionElement conditionElement = conditionTableResults.getSelectionModel().getSelectedItem();
            String name = parentName + "." + item.getValue().getName();
            conditionElement.setLeftExpression(name);
            conditionPopup.hide();
            conditionTableResults.refresh();
        });
    }

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

    @Override
    public void initData(Map<String, Object> userData) {

    }

    @Handler
    public void selectedFieldFormClosedHandler(CustomEvent event) {
        if (!REFRESH_SELECTED_TREE.equals(event.getName())) {
            return;
        }
        selectedConditionsTreeTableContext.applyChanges(event.getChange());
    }
}
