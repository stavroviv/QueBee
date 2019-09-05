package com.querybuilder.home.lab;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;

import javax.swing.*;
import java.util.List;

public class QueryBuilder {
    private JFrame frame;
    private boolean mainForm;
    private MainAction mainAction;

    public QueryBuilder(String text) {
        this(text, true);
    }

    public QueryBuilder(String text, boolean mainForm) {
        this.mainForm = mainForm;
        Statement stmt = null;
        if (text != null && !text.isEmpty()) {
            try {
                stmt = CCJSqlParserUtil.parse(text);
            } catch (JSQLParserException exception) {
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
        JFXPanel fxPanel = new JFXPanel(); // это должно быть перед загрузкой формы..
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
            try {
                Thread.sleep(500);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            frame.setVisible(true);
        });
    }

    public void closeForm() {
        frame.setVisible(false);
    }

    public void closeForm(String result) {
        if (mainForm) {
            mainAction.insertResult(result);
        }
        closeForm();
    }

    public void setMainAction(MainAction mainAction) {
        this.mainAction = mainAction;
    }
}
