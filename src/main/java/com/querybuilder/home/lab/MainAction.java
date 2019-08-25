package com.querybuilder.home.lab;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.util.SelectUtils;

import javax.swing.*;
import java.util.List;

public class MainAction extends AnAction {
    private JFrame frame;
    private MainController mainController;
    private Editor editor;
    private Project project;

    private String getSelectionText(AnActionEvent e) {
        editor = e.getRequiredData(CommonDataKeys.EDITOR);
        project = e.getRequiredData(CommonDataKeys.PROJECT);


        CaretModel caretModel = editor.getCaretModel();

//        String languageTag = "";
//        PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
//        if (file != null) {
//            Language lang = e.getData(CommonDataKeys.PSI_FILE).getLanguage();
////            languageTag = "+[" + lang.getDisplayName().toLowerCase() + "]";
//        }

        String query = "";
        if (caretModel.getCurrentCaret().hasSelection()) {
//            query = caretModel.getCurrentCaret().getSelectedText().replace(' ', '+') + languageTag;
            query = caretModel.getCurrentCaret().getSelectedText();
//         BrowserUtil.browse("https://stackoverflow.com/search?q=" + query);
        }
        return query;
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        String text = getSelectionText(e);
        Statement stmt = null;
        try {
            stmt = CCJSqlParserUtil.parse(text);
        } catch (JSQLParserException e1) {
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
        // Get all the required data from data keys
        final Document document = editor.getDocument();
        // Work off of the primary caret to get the selection info
        Caret primaryCaret = editor.getCaretModel().getPrimaryCaret();
        int start = primaryCaret.getSelectionStart();
        int end = primaryCaret.getSelectionEnd();
        // Replace the selection with a fixed string.
        // Must do this document change in a write action context.
        WriteCommandAction.runWriteCommandAction(
                project, () ->
                        document.replaceString(start, end, resultQuery)
        );
        // De-select the text range that was just replaced
//        primaryCaret.removeSelection();
        frame.setVisible(false);
        System.out.println(resultQuery);
    }
}
