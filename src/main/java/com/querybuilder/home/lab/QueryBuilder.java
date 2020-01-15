package com.querybuilder.home.lab;

import com.intellij.database.dataSource.LocalDataSource;
import com.querybuilder.home.lab.controllers.MainController;
import com.querybuilder.home.lab.domain.TableRow;
import javafx.embed.swing.JFXPanel;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SubSelect;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

import static com.querybuilder.home.lab.utils.Utils.getScene;

public class QueryBuilder {
    private JFrame frame;
    private boolean mainForm;

    private MainAction mainAction;
    private MainController parentController;
    private MainController mainController;
    private TableRow item;

    public QueryBuilder(String text, boolean mainForm, LocalDataSource dataSource) {
        this.mainForm = mainForm;
        this.dataSource = dataSource;

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

        Map<String, Object> data = new HashMap<>();
        data.put("sQuery", stmt);
        data.put("queryBuilder", this);

        JFXPanel fxPanel = new JFXPanel();
        fxPanel.setScene(getScene("/forms/main-form.fxml", data));

        frame = new JFrame("Query builder");
        frame.setContentPane(fxPanel);
        frame.pack();
        frame.setSize(1200, 800);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
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
        Select select = mainController.getsQuery();
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

    public MainController getParentController() {
        return parentController;
    }

    public void setParentController(MainController parentController) {
        this.parentController = parentController;
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