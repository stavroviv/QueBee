package com.querybuilder.querypart;

import com.querybuilder.controllers.MainController;
import com.querybuilder.domain.TableRow;
import com.querybuilder.domain.qparts.OneCte;
import javafx.scene.control.TreeItem;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;

import java.util.Iterator;

import static com.querybuilder.utils.Constants.CTE_ROOT;

public class CTEPart {

    public static void init(MainController controller) {
        controller.getQueryCteColumn().setCellFactory(TextFieldTableCell.forTableColumn());
        controller.getQueryCteColumn().setCellValueFactory(new PropertyValueFactory<>("name"));
    }

    public static void load(MainController controller) {
        // загрузить в дерево таблиц предыдущие CTE
        TreeItem<TableRow> root = controller.getTableFieldsController().getDatabaseTableView().getRoot();
        root.getChildren().forEach(item -> {
            if (item.getValue().isCte()) {
                root.getChildren().remove(item);
            }
        });

        if (controller.getFullQuery().getCteMap().size() == 1) {
            return;
        }

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

        Iterator<String> iterator = controller.getFullQuery().getCteMap().keySet().iterator();
        String currentCte = controller.getCteTabPane().getSelectionModel().getSelectedItem().getId();
        while (iterator.hasNext()) {
            String cteName = iterator.next();
            if (currentCte.equals(cteName)) {
                break;
            }
            TableRow tableRow = new TableRow(cteName);
            tableRow.setRoot(true);
            tableRow.setCte(true);
            TreeItem<TableRow> treeItem = new TreeItem<>(tableRow);
            cteRootItem.getChildren().add(treeItem);

            OneCte oneCte = controller.getFullQuery().getCteMap().get(cteName);
//            if (withItem.getSelectBody() instanceof PlainSelect) {
//                PlainSelect selectBody = (PlainSelect) withItem.getSelectBody();
//                List<SelectItem> selectItems = selectBody.getSelectItems();
            oneCte.getAliasTable().getItems().forEach(item -> {
//                    SelectExpressionItem selectItem = (SelectExpressionItem) item;
//                    String name;
//                    if (selectItem.getAlias() != null) {
//                        name = selectItem.getAlias().getName();
//                    } else {
//                        Column column = (Column) selectItem.getExpression();
//                        String[] split = column.getColumnName().split("\\.");
//                        name = split.length > 1 ? split[1] : split[0]; // FIXME
//                    }
                treeItem.getChildren().add(new TreeItem<>(new TableRow(item.getAlias())));
            });
//            }

        }
    }
}
