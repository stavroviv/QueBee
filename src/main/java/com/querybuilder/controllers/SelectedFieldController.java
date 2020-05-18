package com.querybuilder.controllers;

import com.querybuilder.domain.TableRow;
import com.querybuilder.eventbus.CustomEvent;
import com.querybuilder.eventbus.CustomEventBus;
import com.querybuilder.eventbus.Subscriber;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.querybuilder.utils.Utils.setCellFactory;

public class SelectedFieldController implements Subscriber {
    public static final String FIELD_FORM_CLOSED_EVENT = "ClosedFieldForm";
    private Integer currentRow;
    @FXML
    private Button closeButton;
    @FXML
    private TreeTableView<TableRow> availableFieldsTree;
    @FXML
    private TreeTableColumn<TableRow, TableRow> availableFieldsTreeColumn;
    @FXML
    private HTMLEditor fieldText;

    @FXML
    private void onOkClick(ActionEvent actionEvent) {
        CustomEvent customEvent = new CustomEvent();
        customEvent.setName(FIELD_FORM_CLOSED_EVENT);
        customEvent.setData(getText(fieldText.getHtmlText()));
        customEvent.setCurrentRow(currentRow);
        CustomEventBus.post(customEvent);
        closeForm(actionEvent);
    }

    public static String getText(String htmlText) {
        String result = "";
        Pattern pattern = Pattern.compile("<[^>]*>");
        Matcher matcher = pattern.matcher(htmlText);
        final StringBuffer text = new StringBuffer(htmlText.length());
        while (matcher.find()) {
            matcher.appendReplacement(text, " ");
        }
        matcher.appendTail(text);
        result = text.toString().trim();

        return result
                .replaceAll("&gt;", ">")
                .replaceAll("&lt;", "<")
                .replaceAll("&nbsp", " ")
                .replaceAll(";", System.lineSeparator());
    }

    @FXML
    public void onCloseClick(ActionEvent actionEvent) {
        closeForm(actionEvent);
    }

    private void closeForm(ActionEvent actionEvent) {
        Window window = ((Node) (actionEvent.getSource())).getScene().getWindow();
        if (window instanceof Stage) {
            ((Stage) window).close();
        }
    }

    public void initData(Map<String, Object> userData) {
        availableFieldsTree.setRoot((TreeItem<TableRow>) userData.get("selectedFieldsTree"));
        TableRow selectedItem = (TableRow) userData.get("selectedItem");
        if (userData.get("currentRow") != null) {
            currentRow = (Integer) userData.get("currentRow");
        }
        String text = selectedItem != null ? selectedItem.getName() : " ";
        fieldText.setHtmlText("<html><body contentEditable=\"true\">" + text + "</body></html>");
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

}