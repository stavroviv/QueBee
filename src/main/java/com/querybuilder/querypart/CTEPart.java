package com.querybuilder.querypart;

import com.querybuilder.controllers.MainController;
import com.querybuilder.domain.TableRow;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TreeItem;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.WithItem;

import java.util.List;

import static com.querybuilder.utils.Constants.CTE_ROOT;

public class CTEPart {

    public static void init(MainController controller) {
        controller.getQueryCteColumn().setCellValueFactory(data -> new SimpleStringProperty(data.getValue()));
    }

    public static void load(MainController controller) {
        // загрузить в дерево таблиц предыдущие CTE
        TreeItem<TableRow> root = controller.getDatabaseTableView().getRoot();
        root.getChildren().forEach(item -> {
            if (item.getValue().isCte()) {
                root.getChildren().remove(item);
            }
        });

        List<WithItem> withItemsList = controller.getSQuery().getWithItemsList();
        if (withItemsList == null) {
            return;
        }

        int i = 0;
        int currentCTE = controller.getCteTabPane().getSelectionModel().getSelectedIndex();
        if (currentCTE == 0) {
            return;
        }

        TableRow cteRoot = new TableRow(CTE_ROOT);
        cteRoot.setCteRoot(true);
        cteRoot.setCte(true);
        TreeItem<TableRow> cteRootItem = new TreeItem<>(cteRoot);
        cteRootItem.setExpanded(true);
        root.getChildren().add(0, cteRootItem);

        for (WithItem withItem : withItemsList) {
            if (i == currentCTE) {
                break;
            }
            String cteName = withItem.getName();
            TableRow tableRow = new TableRow(cteName);
            tableRow.setRoot(true);
            tableRow.setCte(true);
            TreeItem<TableRow> treeItem = new TreeItem<>(tableRow);
            cteRootItem.getChildren().add(treeItem);
            if (withItem.getSelectBody() instanceof PlainSelect) {
                PlainSelect selectBody = (PlainSelect) withItem.getSelectBody();
                List<SelectItem> selectItems = selectBody.getSelectItems();
                selectItems.forEach(item -> {
                    SelectExpressionItem selectItem = (SelectExpressionItem) item;
                    String name;
                    if (selectItem.getAlias() != null) {
                        name = selectItem.getAlias().getName();
                    } else {
                        Column column = (Column) selectItem.getExpression();
                        String[] split = column.getColumnName().split("\\.");
                        name = split.length > 1 ? split[1] : split[0]; // FIXME
                    }
                    treeItem.getChildren().add(new TreeItem<>(new TableRow(name)));
                });
            }
            i++;
        }
    }
}
