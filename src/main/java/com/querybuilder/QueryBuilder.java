package com.querybuilder;

import com.intellij.database.dataSource.LocalDataSource;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.querybuilder.controllers.MainController;
import com.querybuilder.domain.TableRow;
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

import static com.querybuilder.utils.Utils.getScene;

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
                JOptionPane.showMessageDialog(null,
                        "Cannot parse query\n" + exception.getCause().getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
        frame.setSize(900, 650);

        DataContext dataContext = DataManager.getInstance().getDataContext();
        Project project = (Project) dataContext.getData(DataConstants.PROJECT);
        IdeFrame ideFrame = WindowManager.getInstance().getIdeFrame(project);

        frame.setLocationRelativeTo(ideFrame.getComponent());
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
        Select select = mainController.getSQuery();
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