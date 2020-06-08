package com.querybuilder.test;

import com.querybuilder.QueryBuilder;
import com.querybuilder.controllers.MainController;
import com.querybuilder.domain.qparts.FullQuery;
import com.querybuilder.eventbus.Subscriber;
import com.querybuilder.utils.Utils;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class QueryBuilderTest {

    static {
        new JFXPanel();
    }

    @NotNull
    private Subscriber loadQuery(String text) throws Exception {
        Statement statement = CCJSqlParserUtil.parse(text);

        URL resource = Utils.class.getResource("/forms/main-form.fxml");
        FXMLLoader fxmlLoader = new FXMLLoader(resource);
        FXMLLoader.setDefaultClassLoader(Utils.class.getClassLoader());

        Parent root = fxmlLoader.load();
        Subscriber controller = fxmlLoader.getController();
        Map<String, Object> data = new HashMap<>();
        data.put("sQuery", statement);
        data.put("queryBuilder", new QueryBuilder());
        controller.initData(data);
        return controller;
    }

    @Test
    public void test1() throws Exception {
        String text = "SELECT crm_bonus_retail.bonus_retail_value AS bonus_retail_value, " +
                "crm_bonus_retail.vendor_id AS vendor_id, " +
                "crm_bonus_retail.bonus_retail_id AS bonus_retail_id, " +
                "crm_bonus_retail.bonus_retail_creation_time AS bonus_retail_creation_time " +
                "FROM crm_bonus_retail " +
                "UNION ALL " +
                "SELECT crm_access.access_name, " +
                "crm_access.access_comment, " +
                "crm_access.access_id, " +
                "crm_advance_payment.advance_payment_id " +
                "FROM crm_access, crm_advance_payment";

        ;
        Subscriber controller = loadQuery(text);

        FullQuery fullQuery = ((MainController) controller).getFullQuery();
        Select query = fullQuery.getQuery();

        Assert.assertEquals(query.toString(), text);
    }

}
