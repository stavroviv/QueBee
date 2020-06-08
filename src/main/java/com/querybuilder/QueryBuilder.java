package com.querybuilder;

import com.intellij.database.console.JdbcConsole;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.querybuilder.controllers.MainController;
import com.querybuilder.domain.TableRow;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import lombok.Data;
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
import static com.querybuilder.utils.Utils.showErrorMessage;

@Data
public class QueryBuilder {
    private JFrame frame;
    private boolean mainForm;

    private MainAction mainAction;
    private MainController parentController;
    private MainController mainController;
    private TableRow item;

    private JdbcConsole console;

    public QueryBuilder() {
    }

    public QueryBuilder(MainAction action, boolean mainForm) {
        this.mainForm = mainForm;
        this.console = JdbcConsole.findConsole(action.getEvent());

        String text = getSelectionText(action);
        Statement statement;
        if (text != null && !text.isEmpty()) {
            try {
                statement = CCJSqlParserUtil.parse(text);
            } catch (JSQLParserException exception) {
                showErrorMessage(exception);
                action.getEvent().getPresentation().setEnabled(true);
                return;
            }
        } else {
            statement = new Select();
        }

        if (!(statement instanceof Select)) {
            return;
        }

        buildForm(action, statement);
    }

    private void buildForm(MainAction action, Statement statement) {
        Map<String, Object> data = new HashMap<>();
        data.put("sQuery", statement);
        data.put("queryBuilder", this);

        Platform.setImplicitExit(false);
        Platform.runLater(() -> {
            try {
                JFXPanel fxPanel = new JFXPanel();
                fxPanel.setScene(getScene("/forms/main-form.fxml", data));
                frame = new JFrame("Query builder");
                frame.setContentPane(fxPanel);
                frame.pack();
                frame.setSize(900, 650);

                IdeFrame ideFrame = WindowManager.getInstance().getIdeFrame(action.getProject());
                frame.setLocationRelativeTo(ideFrame.getComponent());
                frame.setVisible(true);
            } catch (Exception e) {
                showErrorMessage(e);
            } finally {
                action.getEvent().getPresentation().setEnabled(true);
            }
        });
    }

    private String getSelectionText(MainAction action) {
        CaretModel caretModel = action.getEditor().getCaretModel();
        Caret currentCaret = caretModel.getCurrentCaret();
        return currentCaret.hasSelection()
                ? currentCaret.getSelectedText()
                : action.getEvent().getData(CommonDataKeys.PSI_FILE).getText();
    }

    public void closeForm() {
        frame.setVisible(false);
    }

    public void closeForm(String result) {
        //  if (mainForm) {
        mainAction.insertResult(result);
        // } else {
        //   SubSelect subSelect = castResultToSubSelect(item.getName());
//            parentController.insertResult(result, item, subSelect);
        // }
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
}