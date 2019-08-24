package com.querybuilder.home.lab;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;

import javax.swing.*;
import java.util.List;

public class MainAction extends AnAction {
    static JFrame frame = new JFrame("Query builder");
    private String getSelectionText(AnActionEvent e) {

        final Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
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

//        testForm q = new testForm();
        List<SelectItem> selectItems=null;
        if (stmt instanceof Select) {
            Select sQuery = (Select) stmt;
            List<WithItem> withItemsList = sQuery.getWithItemsList();
            PlainSelect selectBody = (PlainSelect) sQuery.getSelectBody();
            selectItems = selectBody.getSelectItems();
        }


        openBuilderForm(selectItems);
    }

    private void openBuilderForm(List<SelectItem> selectItems) {

        JFXPanel fxPanel = new JFXPanel();

        Platform.setImplicitExit(false);
        Platform.runLater(() -> {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/builder-forms/main-builder-form.fxml"));
            fxmlLoader.setController(new MainController(selectItems));


            Parent root1 = null;
            try {
                root1 = fxmlLoader.load();
           } catch (Exception e1) {
                e1.printStackTrace();
                System.out.println(e1);
            }
            fxPanel.setScene(new Scene(root1));

        });

        frame.setContentPane(fxPanel);
        frame.pack();
        frame.setSize(1200, 800);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public static void clos(){
        frame.setVisible(false);
    }
}
