package com.querybuilder.home.lab;

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
import com.intellij.psi.codeStyle.CodeStyleManager;

public class MainAction extends AnAction {
    private Editor editor;
    private Project project;
    private AnActionEvent e;

    @Override
    public void actionPerformed(AnActionEvent e) {
        QueryBuilder qb = new QueryBuilder(getSelectionText(e));
        qb.setMainAction(this);

        JdbcConsole maybeAttachedSession = JdbcConsole.ScriptingJdbcSessionHolder.INSTANCE.getMaybeAttachedSession(e);
        LocalDataSource dataSource = maybeAttachedSession.getDataSource();
        qb.setDataSource(dataSource);
    }

    private String getSelectionText(AnActionEvent e) {
        editor = e.getRequiredData(CommonDataKeys.EDITOR);
        project = e.getRequiredData(CommonDataKeys.PROJECT);
        this.e = e;
        CaretModel caretModel = editor.getCaretModel();
        Caret currentCaret = caretModel.getCurrentCaret();
        return currentCaret.hasSelection()
                ? currentCaret.getSelectedText() : e.getData(CommonDataKeys.PSI_FILE).getText();
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
        });
    }
}
