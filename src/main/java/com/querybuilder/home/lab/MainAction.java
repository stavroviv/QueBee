package com.querybuilder.home.lab;

import com.intellij.database.console.JdbcConsole;
import com.intellij.database.dataSource.LocalDataSource;
import com.intellij.database.model.DatabaseSystem;
import com.intellij.database.model.RawConnectionConfig;
import com.intellij.database.psi.DbDataSource;
import com.intellij.database.psi.DbPsiFacade;
import com.intellij.database.util.DbSqlUtil;
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

import java.util.List;
import java.util.Set;

public class MainAction extends AnAction {

    private Editor editor;
    private Project project;
    private AnActionEvent e;

    private String getSelectionText(AnActionEvent e) {
        editor = e.getRequiredData(CommonDataKeys.EDITOR);
        project = e.getRequiredData(CommonDataKeys.PROJECT);
        this.e = e;
//        DbSqlUtil.SQL_FILE_FILTER(project);
        CaretModel caretModel = editor.getCaretModel();

//        String languageTag = "";
//        PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
//        if (file != null) {
//            Language lang = e.getData(CommonDataKeys.PSI_FILE).getLanguage();
////            languageTag = "+[" + lang.getDisplayName().toLowerCase() + "]";
//        }
// DbPsiFacade is the entry point API for DB/Das model
        List<DbDataSource> dataSources = DbPsiFacade.getInstance(project).getDataSources();
//        DbPsiFacade.getInstance(project).
// get all JDBC URLs
        Set<String> urls = JBIterable.from(dataSources)
                .map(DbDataSource::getDelegate) // unwrap the underlying DatabaseSystem (DDL or JDBC)
                .filterMap(DatabaseSystem::getConnectionConfig)  // get RawConnectionConfig
                .filterMap(RawConnectionConfig::getUrl)          // get JDBC URL
                .toSet();

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

        JdbcConsole maybeAttachedSession = JdbcConsole.ScriptingJdbcSessionHolder.INSTANCE.getMaybeAttachedSession(e);
        LocalDataSource dataSource = maybeAttachedSession.getDataSource();

        QueryBuilder qb = new QueryBuilder(text);
        qb.setMainAction(this);
        qb.setDataSource(dataSource);
    }

    public void insertResult(String resultQuery) {
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
