package com.querybuilder.test;

import com.querybuilder.QueryBuilder;
import com.querybuilder.controllers.MainController;
import com.querybuilder.domain.TableRow;
import com.querybuilder.domain.qparts.FullQuery;
import com.querybuilder.domain.qparts.OneCte;
import com.querybuilder.eventbus.Subscriber;
import com.querybuilder.utils.Utils;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableView;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static com.querybuilder.utils.Constants.CTE_0;
import static com.querybuilder.utils.Constants.UNION_0;

public class QueryBuilderTest {

    static {
        new JFXPanel();
    }

    @NotNull
    private MainController loadQuery(String text) throws Exception {
        Statement statement = CCJSqlParserUtil.parse(text);

        URL resource = Utils.class.getResource("/forms/main-form.fxml");
        FXMLLoader fxmlLoader = new FXMLLoader(resource);
        FXMLLoader.setDefaultClassLoader(Utils.class.getClassLoader());
        fxmlLoader.load();

        Subscriber controller = fxmlLoader.getController();
        Map<String, Object> data = new HashMap<>();
        data.put("sQuery", statement);
        data.put("queryBuilder", new QueryBuilder());
        controller.initData(data);
        return (MainController) controller;
    }

    @Test
    public void aliasesSave() throws Exception {
        String text = "SELECT crm_bonus_retail.bonus_retail_value AS bonus, " +
                "crm_bonus_retail.vendor_id, " +
                "crm_bonus_retail.bonus_retail_id, " +
                "crm_bonus_retail.bonus_retail_creation_time AS bonus_retail " +
                "FROM crm_bonus_retail " +
                "UNION ALL " +
                "SELECT crm_access.access_name, " +
                "crm_access.access_comment, " +
                "crm_access.access_id, " +
                "crm_advance_payment.advance_payment_id " +
                "FROM crm_access, crm_advance_payment";

        Select query = loadQuery(text).getFullQuery().getQuery();

        Assert.assertEquals(text, query.toString());
    }

    @Test
    public void aliasesSaveWithEmpty() throws Exception {
        String text = "SELECT crm_bonus_retail.bonus_retail_value AS bonusOne, " +
                "NULL AS bestBonus " +
                "FROM crm_bonus_retail " +
                "UNION ALL " +
                "SELECT crm_access.access_name, " +
                "crm_access.access_comment " +
                "FROM crm_access, crm_advance_payment";

        Select query = loadQuery(text).getFullQuery().getQuery();

        Assert.assertEquals(text, query.toString());
    }

    @Test
    public void loadWithoutTableName() throws Exception {
        String text = "SELECT bonus_retail_value AS bonus_retail_value, " +
                "vendor_id AS vendor_id " +
                "FROM crm_bonus_retail";

        Select query = loadQuery(text).getFullQuery().getQuery();

        String expected = "SELECT crm_bonus_retail.bonus_retail_value, " +
                "crm_bonus_retail.vendor_id " +
                "FROM crm_bonus_retail";
        Assert.assertEquals(expected, query.toString());
    }

    @Test
    public void loadWithGroupBy() throws Exception {
        String text = "SELECT vendor_id FROM crm_bonus_retail GROUP BY vendor_id";
        Select query = loadQuery(text).getFullQuery().getQuery();
        String expected = "SELECT crm_bonus_retail.vendor_id FROM crm_bonus_retail GROUP BY crm_bonus_retail.vendor_id";
        Assert.assertEquals(expected, query.toString());

        text = "SELECT vendor_id, site_id FROM crm_bonus_retail GROUP BY vendor_id";
        query = loadQuery(text).getFullQuery().getQuery();
        expected = "SELECT crm_bonus_retail.vendor_id, crm_bonus_retail.site_id " +
                "FROM crm_bonus_retail GROUP BY crm_bonus_retail.vendor_id, crm_bonus_retail.site_id";
        Assert.assertEquals(expected, query.toString());

        text = "SELECT vendor_id, site_id FROM crm_bonus_retail, crm_vendor GROUP BY vendor_id";
        try {
            loadQuery(text).getFullQuery().getQuery();
        } catch (Exception e) {
            Assert.assertEquals("Ambiguous column reference: vendor_id", e.getMessage());
        }
    }

    @Test
    public void loadWithFunction() throws Exception {
        String text = "SELECT SUM(bonus_retail_value), " +
                "vendor_id " +
                "FROM crm_bonus_retail " +
                "GROUP BY vendor_id";

        Select query = loadQuery(text).getFullQuery().getQuery();

        String expected = "SELECT SUM(crm_bonus_retail.bonus_retail_value), " +
                "crm_bonus_retail.vendor_id " +
                "FROM crm_bonus_retail " +
                "GROUP BY crm_bonus_retail.vendor_id";
        Assert.assertEquals(expected, query.toString());
    }

    @Test
    public void loadSaveWithNull() throws Exception {
        String text = "SELECT bonus_retail_value, " +
                "vendor_id," +
                "vendor_id2 " +
                "FROM crm_bonus_retail " +
                "UNION ALL " +
                "SELECT crm_access.access_name " +
                "FROM crm_access";

        FullQuery fullQuery = loadQuery(text).getFullQuery();
        Select query = fullQuery.getQuery();

        String expected = "SELECT crm_bonus_retail.bonus_retail_value, " +
                "crm_bonus_retail.vendor_id, " +
                "crm_bonus_retail.vendor_id2 " +
                "FROM crm_bonus_retail " +
                "UNION ALL " +
                "SELECT crm_access.access_name, NULL, NULL " +
                "FROM crm_access";
        Assert.assertEquals(expected, query.toString());
    }

    @Test
    public void loadWithNull() throws Exception {
        String text = "SELECT bonus_retail_value, " +
                "vendor_id," +
                "NULL AS test " +
                "FROM crm_bonus_retail " +
                "UNION ALL " +
                "SELECT crm_access.access_name, NULL, vendor_id " +
                "FROM crm_access";

        FullQuery fullQuery = loadQuery(text).getFullQuery();
        OneCte cte_0 = fullQuery.getCteMap().get("CTE_0");
        TableView<TableRow> fieldTable = cte_0.getUnionMap().get(UNION_0).getFieldTable();
        Assert.assertEquals(2, fieldTable.getItems().size());

        fieldTable = cte_0.getUnionMap().get("UNION_1").getFieldTable();
        Assert.assertEquals(2, fieldTable.getItems().size());

        Select query = fullQuery.getQuery();
        String expected = "SELECT crm_bonus_retail.bonus_retail_value, " +
                "crm_bonus_retail.vendor_id, " +
                "NULL AS test " +
                "FROM crm_bonus_retail " +
                "UNION ALL " +
                "SELECT crm_access.access_name, " +
                "NULL, " +
                "crm_access.vendor_id FROM crm_access";
        Assert.assertEquals(expected, query.toString());
    }

    @Test
    public void loadUnions() throws Exception {
        String text = "SELECT crm_bonus_retail.bonus_retail_value AS bonusOne, " +
                "NULL AS bestBonus " +
                "FROM crm_bonus_retail " +
                "UNION ALL " +
                "SELECT crm_access.access_name, " +
                "crm_access.access_comment " +
                "FROM crm_access, crm_advance_payment " +
                "UNION ALL " +
                "SELECT crm_access.access_name, " +
                "crm_access.access_comment " +
                "FROM crm_access, crm_advance_payment";

        FullQuery fullQuery = loadQuery(text).getFullQuery();
        TableView<TableRow> unionTable = fullQuery.getCteMap().get(CTE_0).getUnionTable();
        Assert.assertEquals(3, unionTable.getItems().size());

        text = "WITH CTE_0 AS (SELECT crm_bonus_retail.bonus_retail_value AS bonusOne, " +
                "NULL AS bestBonus " +
                "FROM crm_bonus_retail " +
                "UNION ALL " +
                "SELECT crm_access.access_name, " +
                "crm_access.access_comment " +
                "FROM crm_access, crm_advance_payment " +
                "UNION ALL " +
                "SELECT crm_access.access_name, " +
                "crm_access.access_comment " +
                "FROM crm_access, crm_advance_payment) " +
                "SELECT CTE_0.bonusOne from CTE_0";

        fullQuery = loadQuery(text).getFullQuery();
        unionTable = fullQuery.getCteMap().get(CTE_0).getUnionTable();
        TableView<TableRow> cte_1 = fullQuery.getCteMap().get("CTE_1").getUnionTable();
        Assert.assertEquals(3, unionTable.getItems().size());
        Assert.assertEquals(1, cte_1.getItems().size());
    }
}
