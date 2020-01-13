package com.querybuilder.home.lab.controllers;

import com.querybuilder.home.lab.domain.TableRow;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
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
    private TextArea fieldText;

    public void onOkClick(ActionEvent actionEvent) {
        Window window = ((Node) (actionEvent.getSource())).getScene().getWindow();
        if (window instanceof Stage) {
            ((Stage) window).close();
        }
    }

    public void initialize() {
        setCellFactory(availableFieldsTreeColumn);
        setDragAndDrop();
    }

    private void setDragAndDrop() {
        availableFieldsTree.setOnDragDetected(event -> {
            /* allow any transfer mode */
            Dragboard db = availableFieldsTree.startDragAndDrop(TransferMode.ANY);
//            db.setDragView(new Text("sdfsdf").snapshot(null, null), event.getX(), event.getY());
            /* put a string on dragboard */
            ClipboardContent content = new ClipboardContent();
            TableRow selectedItem = ((TreeTableView<TableRow>) event.getSource())
                    .getSelectionModel()
                    .getSelectedItem()
                    .getValue();
            String name = selectedItem.getName();
            content.putString(name);
            db.setContent(content);
            event.consume();
        });
        fieldText.setOnDragOver(event -> {
            /* accept it only if it is  not dragged from the same node
             * and if it has a string data */
            if (event.getGestureSource() != fieldText && event.getDragboard().hasString()) {
                /* allow for both copying and moving, whatever user chooses */
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
//                    int anchor = fieldText.getAnchor();
//                    fieldText.setText(event.getDragboard().getString());
            }
//            fieldText.requestFocus();
            event.consume();
        });

        fieldText.setOnDragEntered(event -> {
            /* the drag-and-drop gesture entered the target */
            System.out.println("onDragEntered");
            /* show to the user that it is an actual gesture target */
            if (event.getGestureSource() != fieldText &&
                    event.getDragboard().hasString()) {
//                    fieldText.setmo(Color.GREEN);
//                fieldText.positionCaret((int) event.getX());
//                fieldText.requestFocus();
                Platform.runLater(() -> fieldText.requestFocus());
            }

            event.consume();
        });

        fieldText.setOnDragDropped(event -> {
            /* if there is a string data on dragboard, read it and use it */
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString()) {
//                int caretPosition = fieldText.getCaretPosition();
//                fieldText.setText(fieldText.getText(0, caretPosition) +
//                                db.getString() +
//                                fieldText.getText(caretPosition, fieldText.getLength())
//                );
                fieldText.insertText(fieldText.getCaretPosition(), db.getString());
                success = true;
            }
            /* let the source know whether the string was successfully
             * transferred and used */
            event.setDropCompleted(success);
            event.consume();
        });

    }

    public void initData(Map<String, Object> userData) {
        availableFieldsTree.setRoot((TreeItem<TableRow>) userData.get("selectedFieldsTree"));
    }
}
