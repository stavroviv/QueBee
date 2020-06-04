package com.querybuilder.utils;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.RelativePoint;
import com.querybuilder.controllers.MainController;
import com.querybuilder.domain.TableRow;
import com.querybuilder.eventbus.Subscriber;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.PlainSelect;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.querybuilder.utils.Constants.*;

public class Utils {

    public static PlainSelect getEmptySelect() {
        return new PlainSelect();
    }

    public static void showMessage(String message) {
        DataContext dataContext = DataManager.getInstance().getDataContext();
        Project project = (Project) dataContext.getData(DataConstants.PROJECT);
        IdeFrame ideFrame = WindowManager.getInstance().getIdeFrame(project);
        FileDocumentManager.getInstance().saveAllDocuments();

        String html = "<html><body>" + message + "</body></html>";
        JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(html, MessageType.INFO, null)
                .setFadeoutTime(10_000)
                .createBalloon()
                .show(RelativePoint.getCenterOf(ideFrame.getStatusBar().getComponent()), Balloon.Position.above);
    }

    public static void setEmptyHeader(Control control) {
        control.widthProperty().addListener((ov, t, t1) -> {
            Pane header = (Pane) control.lookup("TableHeaderRow");
            if (header != null && header.isVisible()) {
                header.setMaxHeight(0);
                header.setMinHeight(0);
                header.setPrefHeight(0);
                header.setVisible(false);
                header.setManaged(false);
            }
        });
    }

    public static void setDefaultSkin(PopupControl popup, Control control, Control cell) {
        popup.setSkin(new Skin<Skinnable>() {
            @Override
            public Skinnable getSkinnable() {
                return null;
            }

            @Override
            public Node getNode() {
                control.setMinWidth(cell.getWidth());
                control.setMaxWidth(cell.getWidth());
                return control;
            }

            @Override
            public void dispose() {
            }
        });
    }

    public static void setCellFactory(TreeTableColumn<com.querybuilder.domain.TableRow, TableRow> tablesViewColumn) {
        tablesViewColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getValue()));
        tablesViewColumn.setCellFactory(ttc -> new CustomCell());
    }

    public static ObservableList<String> getColumns(MainController controller, String table, AtomicReference<Boolean> isCte) {
        List<String> resultColumns = new ArrayList<>();
        List<String> dbColumns = controller.getDbElements().get(table);
        if (dbColumns != null) {
            resultColumns.addAll(dbColumns);
            return FXCollections.observableArrayList(resultColumns);
        }

        // CTE
        TreeTableView<TableRow> databaseTableView = controller.getTableFieldsController().getDatabaseTableView();
        ObservableList<TreeItem<TableRow>> tables = databaseTableView.getRoot().getChildren();
        if (tables.size() > 0 && tables.get(0).getValue().getName().equals(CTE_ROOT)) {
            ObservableList<TreeItem<TableRow>> cte = tables.get(0).getChildren();
            for (TreeItem<TableRow> item : cte) {
                if (!item.getValue().getName().equals(table)) {
                    continue;
                }
                isCte.set(true);
                item.getChildren().forEach(col -> resultColumns.add(col.getValue().getName()));
                break;
            }
        }
        return FXCollections.observableArrayList(resultColumns);
    }

    public static void openForm(String formName, String title, Map<String, Object> userData) {
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(getScene(formName, userData));
        stage.setOnCloseRequest((e) -> {
            System.out.println("sdfsdf");
        });
        stage.show();
    }

    public static Scene getScene(String formName, Map<String, Object> userData) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Utils.class.getResource(formName));
            FXMLLoader.setDefaultClassLoader(Utils.class.getClassLoader());
            Parent root = fxmlLoader.load();
            Subscriber controller = fxmlLoader.getController();
            controller.initData(userData);
            return new Scene(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean doubleClick(MouseEvent e) {
        return e.getClickCount() == 2 && e.isPrimaryButtonDown();
    }

    public static <E> void addElementBeforeTree(ObservableList<E> list, E row) {
        // try {
        if (list.isEmpty()) {
            list.add(row);
        } else {
            list.add(list.size() - 1, row);
        }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    public static String getNameFromColumn(Column column) {
        return column.getTable().getName() + "." + column.getColumnName();
    }

    public static void setCellSelectionEnabled(TableView<TableRow> table) {
        table.getSelectionModel().setCellSelectionEnabled(true);
    }

    public static void setStringColumnFactory(TableColumn<TableRow, String> resultsColumn) {
        setStringColumnFactory(resultsColumn, false);
    }

    public static void setStringColumnFactory(TableColumn<TableRow, String> resultsColumn, boolean editable) {
        resultsColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        resultsColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        resultsColumn.setEditable(editable);
    }

    public static void setComboBoxColumnFactory(TableColumn<TableRow, String> column, String... items) {
        column.setEditable(true);
        column.setCellFactory(
                ComboBoxTableCell.forTableColumn(FXCollections.observableArrayList(items))
        );
        column.setCellValueFactory(cellData -> cellData.getValue().comboBoxValueProperty());
    }

    public static void setResultsTableSelectHandler(TableView<TableRow> groupTableResults, TreeTableView<TableRow> groupFieldsTree) {
        groupTableResults.setOnMousePressed(e -> {
            if (doubleClick(e)) {
                makeDeselect(groupTableResults, groupFieldsTree);
            }
        });
    }

    // DESELECT

    public static void makeDeselectAll(TableView<TableRow> resultsTable, TreeTableView<TableRow> fieldsTree) {
        for (TableRow item : resultsTable.getItems()) {
            TreeItem<TableRow> treeItem = new TreeItem<>(item);
            addElementBeforeTree(fieldsTree.getRoot().getChildren(), treeItem);
        }
        resultsTable.getItems().clear();
    }

    public static void makeDeselect(TableView<TableRow> resultsTable, TreeTableView<TableRow> fieldsTree, TableRow item) {
        if (item == null) {
            item = resultsTable.getSelectionModel().getSelectedItem();
        }
        if (resultsTable.getId().equals("groupTableAggregates")
                && resultsTable.getSelectionModel().getSelectedCells().get(0).getColumn() == 1) {
            return;
        }
        TableRow tableRow = new TableRow(item.getName());
        if (tableRow.isNotSelectable()) {
            return;
        }
        TreeItem<TableRow> treeItem = new TreeItem<>(tableRow);
        addElementBeforeTree(fieldsTree.getRoot().getChildren(), treeItem);
        resultsTable.getItems().remove(item);
    }

    public static void makeDeselect(TableView<TableRow> resultsTable, TreeTableView<TableRow> fieldsTree) {
        makeDeselect(resultsTable, fieldsTree, null);
    }

    // SELECT
    public static void makeSelectAll(TreeTableView<TableRow> fieldsTree, TableView<TableRow> resultsTable, String value) {
        List<TreeItem<TableRow>> deleteItems = new ArrayList<>();
        for (TreeItem<TableRow> item : fieldsTree.getRoot().getChildren()) {
            if (item.getValue().getName().equals(ALL_FIELDS)) {
                break;
            }
            deleteItems.add(item);
            TableRow tableRow = TableRow.tableRowFromValue(item.getValue());
            if (value != null) {
                tableRow.setComboBoxValue(value);
            }
            resultsTable.getItems().add(tableRow);
        }
        for (TreeItem<TableRow> item : deleteItems) {
            fieldsTree.getRoot().getChildren().remove(item);
        }
    }

    public static void makeSelectAll(TreeTableView<TableRow> fieldsTree, TableView<TableRow> resultsTable) {
        makeSelectAll(fieldsTree, resultsTable, "");
    }

    public static void makeSelect(TreeTableView<TableRow> fieldsTree, TableView<TableRow> table) {
        makeSelect(fieldsTree, table, null);
    }

    public static void makeSelect(TreeTableView<TableRow> fieldsTree, TableView<TableRow> table, String value) {
        makeSelect(fieldsTree, table, null, value);
    }

    public static void makeSelect(TreeTableView<TableRow> fieldsTree, TableView<TableRow> table,
                                  TreeItem<TableRow> item, String value) {
        if (item == null) {
            item = fieldsTree.getSelectionModel().getSelectedItem();
        }

        if (item.getChildren().size() > 0) {
            return;
        }
        String name = item.getValue().getName();

        TreeItem<TableRow> parent = item.getParent();
        String parentName = parent.getValue().getName();
        TableRow newItem;
        if (!parentName.equals(DATABASE_TABLE_ROOT)) {
            name = parentName + "." + name;
            newItem = new TableRow(name);
        } else {
            newItem = TableRow.tableRowFromValue(item.getValue());
        }

        if (value != null) {
            newItem.setComboBoxValue(value);
        }
        table.getItems().add(newItem);
        fieldsTree.getRoot().getChildren().remove(item);
    }

    public static void setTreeSelectHandler(TreeTableView<TableRow> fieldsTree, TableView<TableRow> table) {
        setTreeSelectHandler(fieldsTree, table, "");
    }

    public static void setTreeSelectHandler(TreeTableView<TableRow> tree, TableView<TableRow> table, String value) {
        tree.setOnMousePressed(e -> {
            if (doubleClick(e)) {
                makeSelect(tree, table, value);
            }
        });
    }
}
