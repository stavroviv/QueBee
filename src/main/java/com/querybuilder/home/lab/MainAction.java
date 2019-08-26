package com.querybuilder.home.lab;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
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
import net.sf.jsqlparser.statement.select.WithItem;

import javax.swing.*;
import java.util.List;

public class MainAction extends AnAction {
    private JFrame frame;
    private MainController mainController;
    private Editor editor;
    private Project project;
    private AnActionEvent e;

    private String getSelectionText(AnActionEvent e) {
        editor = e.getRequiredData(CommonDataKeys.EDITOR);
        project = e.getRequiredData(CommonDataKeys.PROJECT);
        this.e = e;

        CaretModel caretModel = editor.getCaretModel();

//        String languageTag = "";
//        PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
//        if (file != null) {
//            Language lang = e.getData(CommonDataKeys.PSI_FILE).getLanguage();
////            languageTag = "+[" + lang.getDisplayName().toLowerCase() + "]";
//        }

        String query = "";
        if (caretModel.getCurrentCaret().hasSelection()) {
            query = caretModel.getCurrentCaret().getSelectedText();
        }

        if (query.isEmpty()) {
            PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
            query = file.getText();
        }
        return query;
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        String text = getSelectionText(e);
        Statement stmt = null;
        try {
            stmt = CCJSqlParserUtil.parse(text);
        } catch (JSQLParserException exception) {
            exception.printStackTrace();
        }
        List<SelectItem> selectItems = null;
        if (stmt instanceof Select) {
            Select sQuery = (Select) stmt;
            List<WithItem> withItemsList = sQuery.getWithItemsList();
            PlainSelect selectBody = (PlainSelect) sQuery.getSelectBody();
            selectItems = selectBody.getSelectItems();
//            selectBody.getGroupBy().addGroupingSet();
//            TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
////            SelectUtils.buildSelectFromTable()
//            List<String> tableList = tablesNamesFinder.getTableList(stmt);
            openBuilderForm(sQuery);
        }


    }

    private void openBuilderForm(Select sQuery) {
//        if (frame != null) {
//            mainController.init(sQuery);
//            frame.setVisible(true);
//            return;
//        }
        mainController = new MainController(sQuery, this);
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
        });
        frame = new JFrame("Query builder");
        frame.setContentPane(fxPanel);
        frame.pack();
        frame.setSize(1200, 800);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public void clos() {
        frame.setVisible(false);
    }

    public void clos(String resultQuery) {
        ApplicationManager.getApplication().invokeLater(() -> {
            final Document document = editor.getDocument();
            // Work off of the primary caret to get the selection info
            Caret primaryCaret = editor.getCaretModel().getPrimaryCaret();
            int start = primaryCaret.getSelectionStart();
            int end = primaryCaret.getSelectionEnd();
            // Replace the selection with a fixed string.
            // Must do this document change in a write action context.
            WriteCommandAction.runWriteCommandAction(
                    project, () ->
                    {
                        PsiElement data = e.getData(LangDataKeys.PSI_FILE);
                        if (start != end) {
                            document.replaceString(start, end, resultQuery);
                        } else {
                            document.replaceString(0, document.getTextLength(), resultQuery);
                        }
                        CodeStyleManager.getInstance(project).reformat(data);
                    }
            );
            frame.setVisible(false);

        });
    }
}
