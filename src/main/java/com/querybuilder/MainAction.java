package com.querybuilder;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import javafx.embed.swing.JFXPanel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class MainAction extends AnAction {
    private Editor editor;
    private Project project;
    private AnActionEvent event;

    static {
        new JFXPanel();
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        event.getPresentation().setEnabled(false);
        this.editor = event.getRequiredData(CommonDataKeys.EDITOR);
        this.project = event.getRequiredData(CommonDataKeys.PROJECT);
        this.event = event;

        QueryBuilder qb = new QueryBuilder(this, true);
        qb.setMainAction(this);
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
                PsiElement data = event.getData(LangDataKeys.PSI_FILE);
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