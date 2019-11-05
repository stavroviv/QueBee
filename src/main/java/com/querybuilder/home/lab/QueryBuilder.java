package com.querybuilder.home.lab;

import com.intellij.database.dataSource.LocalDataSource;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.util.Duration;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SubSelect;

import javax.swing.*;

public class QueryBuilder {
    private JFrame frame;
    private boolean mainForm;

    private MainAction mainAction;
    private MainController parentController;
    private MainController controller;
    private TableRow item;

    public MainController getParentController() {
        return parentController;
    }

    public void setParentController(MainController parentController) {
        this.parentController = parentController;
    }


    public QueryBuilder(String text) {
        this(text, true);
    }

    public QueryBuilder(String text, boolean mainForm) {
        this.mainForm = mainForm;
        Statement stmt;
        if (text != null && !text.isEmpty()) {
            try {
                stmt = CCJSqlParserUtil.parse(text);
            } catch (JSQLParserException exception) {
                System.out.println(text);
                exception.printStackTrace();
                return;
            }
        } else {
            stmt = new Select();
        }

        if (!(stmt instanceof Select)) {
            return;
        }
        Select sQuery = (Select) stmt;

        MainController mainController = new MainController(sQuery, this);
        this.controller = mainController;

        JFXPanel fxPanel = new JFXPanel(); // это должно быть перед загрузкой формы
        Platform.setImplicitExit(false);
        Platform.runLater(() -> {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/builder-forms/main-builder-form.fxml"));
            fxmlLoader.setController(mainController);
            Parent root1 = null;
            try {
                root1 = fxmlLoader.load();
            } catch (Exception e1) {
                e1.printStackTrace();
                System.out.println(e1);
            }
            fxPanel.setScene(new Scene(root1));

            frame = new JFrame("Query builder");
            frame.setContentPane(fxPanel);
            frame.pack();
            frame.setSize(1200, 800);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            root1.setOpacity(0.1f);
            FadeTransition ft = new FadeTransition(Duration.millis(500), root1);
            ft.setToValue(1);
            ft.play();
        });
    }


    public void closeForm() {
        frame.setVisible(false);
    }

    public void closeForm(String result) {
        if (mainForm) {
            mainAction.insertResult(result);
        } else {
            SubSelect subSelect = castResultToSubSelect(item.getName());
            parentController.insertResult(result, item, subSelect);
        }
        closeForm();
    }

    private SubSelect castResultToSubSelect(String alias) {
        SubSelect result = new SubSelect();
        Select select = this.controller.getsQuery();
        result.setSelectBody(select.getSelectBody());
        result.setWithItemsList(select.getWithItemsList());
        result.setAlias(new Alias(alias));
        return result;
    }

    public void setMainAction(MainAction mainAction) {
        this.mainAction = mainAction;
    }

    public MainAction getMainAction() {
        return mainAction;
    }

    public void setItem(TableRow item) {
        this.item = item;
    }

    public LocalDataSource getDataSource() {
        return dataSource;
    }

    private LocalDataSource dataSource;

    public void setDataSource(LocalDataSource dataSource) {
        this.dataSource = dataSource;
    }
}
