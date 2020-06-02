package com.querybuilder;

import com.intellij.database.console.JdbcConsole;
import com.intellij.database.dataSource.LocalDataSource;
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
import javafx.embed.swing.JFXPanel;

public class MainAction extends AnAction {
    private Editor editor;
    private Project project;
    private AnActionEvent mainEvent;

    static {
        new JFXPanel();
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        this.editor = event.getRequiredData(CommonDataKeys.EDITOR);
        this.project = event.getRequiredData(CommonDataKeys.PROJECT);
        this.mainEvent = event;
        JdbcConsole console = JdbcConsole.findConsole(event);
        LocalDataSource dataSource = console.getDataSource();
        QueryBuilder qb = new QueryBuilder(getSelectionText(event), true, dataSource);
        qb.setMainAction(this);
    }

    private String getSelectionText(AnActionEvent event) {
        CaretModel caretModel = editor.getCaretModel();
        Caret currentCaret = caretModel.getCurrentCaret();
        return currentCaret.hasSelection()
                ? currentCaret.getSelectedText() : event.getData(CommonDataKeys.PSI_FILE).getText();
    }

    void insertResult(String resultQuery) {
        ApplicationManager.getApplication().invokeLater(() -> {
            final Document document = editor.getDocument();
            // Work off of the primary caret to get the selection info
            Caret primaryCaret = editor.getCaretModel().getPrimaryCaret();
            int start = primaryCaret.getSelectionStart();
            int end = primaryCaret.getSelectionEnd();
            // Replace the selection with a fixed string.
            // Must do this document change in a write action context.
            WriteCommandAction.runWriteCommandAction(project, () -> {
                PsiElement data = mainEvent.getData(LangDataKeys.PSI_FILE);
                if (start != end) {
                    document.replaceString(start, end, resultQuery);
                } else {
                    document.replaceString(0, document.getTextLength(), resultQuery);
                }
                CodeStyleManager.getInstance(project).reformatText((PsiFile) data, 0, document.getTextLength());
            });
        });
    }
}