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
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.querybuilder.utils.Constants.CTE_ROOT;

public class Utils {

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

    public static ObservableList<String> getColumns(MainController controller, String tableName, AtomicReference<Boolean> isCte) {
        List<String> resultColumns = new ArrayList<>();
        List<String> dbColumns = controller.getDbElements().get(tableName);
        if (dbColumns != null) {
            resultColumns.addAll(dbColumns);
            return FXCollections.observableArrayList(resultColumns);
        }

        // CTE
        ObservableList<TreeItem<TableRow>> tables = controller.getDatabaseTableView().getRoot().getChildren();
        if (tables.size() > 0 && tables.get(0).getValue().getName().equals(CTE_ROOT)) {
            ObservableList<TreeItem<TableRow>> cte = tables.get(0).getChildren();
            for (TreeItem<TableRow> item : cte) {
                if (!item.getValue().getName().equals(tableName)) {
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
}
