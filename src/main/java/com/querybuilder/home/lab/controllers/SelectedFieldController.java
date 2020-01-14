package com.querybuilder.home.lab.controllers;

import com.querybuilder.home.lab.domain.TableRow;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.web.HTMLEditor;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Map;

import static com.querybuilder.home.lab.utils.Utils.setCellFactory;

public class SelectedFieldController {
    @FXML
    private Button closeButton;
    @FXML
    private TreeTableView<TableRow> availableFieldsTree;
    @FXML
    private TreeTableColumn<TableRow, TableRow> availableFieldsTreeColumn;
    @FXML
    private HTMLEditor fieldText;

    public void onOkClick(ActionEvent actionEvent) {
        Window window = ((Node) (actionEvent.getSource())).getScene().getWindow();
        if (window instanceof Stage) {
            ((Stage) window).close();
        }
    }

    public void initialize() {
        setCellFactory(availableFieldsTreeColumn);
        setDragAndDrop();
        hideToolbar();
    }

    private void hideToolbar() {
        fieldText.lookup(".top-toolbar").setManaged(false);
        fieldText.lookup(".top-toolbar").setVisible(false);
        fieldText.lookup(".bottom-toolbar").setManaged(false);
        fieldText.lookup(".bottom-toolbar").setVisible(false);
    }

    private void setDragAndDrop() {
        availableFieldsTree.setOnDragDetected(event -> {
            Dragboard db = availableFieldsTree.startDragAndDrop(TransferMode.COPY);
            TableRow selectedItem = ((TreeTableView<TableRow>) event.getSource())
                    .getSelectionModel()
                    .getSelectedItem()
                    .getValue();
            String name = selectedItem.getName();
            ClipboardContent content = new ClipboardContent();
            content.putString(name);
            db.setContent(content);
            event.consume();
        });
    }

    public void initData(Map<String, Object> userData) {
        availableFieldsTree.setRoot((TreeItem<TableRow>) userData.get("selectedFieldsTree"));
    }
}