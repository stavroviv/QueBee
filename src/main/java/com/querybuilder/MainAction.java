package com.querybuilder;

import com.intellij.database.console.JdbcConsole;
import com.intellij.database.dataSource.LocalDataSource;
import com.intellij.database.model.DasObject;
import com.intellij.database.model.ObjectKind;
import com.intellij.database.util.ObjectPath;
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
import com.intellij.util.containers.JBIterable;
import javafx.embed.swing.JFXPanel;

public class MainAction extends AnAction {
    private Editor editor;
    private Project project;
    private AnActionEvent e;

    static {
        new JFXPanel();
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        JdbcConsole console = JdbcConsole.findConsole(e);
        LocalDataSource dataSource = console.getDataSource();

        ObjectPath currentNamespace = console.getCurrentNamespace();
        //   System.out.println(currentNamespace);
        JBIterable<? extends DasObject> modelRoots = dataSource.getModel().getModelRoots();
//        for (DasObject modelRoot : modelRoots) {
//            System.out.println(modelRoot.getName());
//        }
        String sqlDialect = dataSource.getDatabaseDriver().getSqlDialect();
        if (sqlDialect.equals("PostgreSQL")) {
            //   modelRoots.find(x->x.getName().equals(currentNamespace.getName())).getDasChildren()
            DasObject dasObject = modelRoots.find(x -> x.getName().equals(currentNamespace.getName()));
            JBIterable<? extends DasObject> dasChildren = dasObject.getDasChildren(ObjectKind.SCHEMA);
            DasObject dasObject1 = dasChildren.find(x -> x.getName().equals("public: schema"));
            for (DasObject dasChild : dasChildren) {
                System.out.println(dasChild);
                // (PgImplModel.) dasChild;
            }
            JBIterable<? extends DasObject> dasChildren1 = dasObject1.getDasChildren(ObjectKind.TABLE);
            for (DasObject object : dasChildren1) {
                System.out.println(object);
            }
        }

        QueryBuilder qb = new QueryBuilder(getSelectionText(e), true, dataSource);
        qb.setMainAction(this);
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
                        CodeStyleManager.getInstance(project).reformatText((PsiFile) data, 0, document.getTextLength());
                    }
            );
        });
    }
}