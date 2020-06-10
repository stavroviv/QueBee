package com.querybuilder.querypart;

import com.querybuilder.domain.JoinConditionCell;
import com.querybuilder.domain.LinkElement;
import com.querybuilder.domain.LinkTableCell;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SubSelect;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class Links extends AbstractQueryPart {
    @FXML
    private TableView<LinkElement> linkTable;
    @FXML
    private TableColumn<LinkElement, LinkElement> linkTableColumnTable1;
    @FXML
    private TableColumn<LinkElement, LinkElement> linkTableColumnTable2;
    @FXML
    private TableColumn<LinkElement, Boolean> linkTableAllTable1;
    @FXML
    private TableColumn<LinkElement, Boolean> linkTableAllTable2;
    @FXML
    private TableColumn<LinkElement, Boolean> linkTableCustom;
    @FXML
    private TableColumn<LinkElement, LinkElement> linkTableJoinCondition;


    @FXML
    protected void addLinkElement(ActionEvent event) {
        LinkElement newRow = new LinkElement(mainController);
        linkTable.getItems().add(newRow);
    }

    @FXML
    protected void copyLinkElement(ActionEvent event) {
        LinkElement selectedItem = linkTable.getSelectionModel().getSelectedItem();
        linkTable.getItems().add(selectedItem.clone());
    }

    @FXML
    protected void deleteLinkElement(ActionEvent event) {
        LinkElement selectedItem = linkTable.getSelectionModel().getSelectedItem();
        linkTable.getItems().remove(selectedItem);
    }

    @FXML
    @Override
    public void initialize() {

        linkTable.setEditable(true);

        linkTableAllTable1.setCellFactory(tc -> new CheckBoxTableCell<>());
        linkTableAllTable2.setCellFactory(tc -> new CheckBoxTableCell<>());

        linkTableCustom.setCellFactory(column -> new CheckBoxTableCell<>());
        linkTableCustom.setCellValueFactory(cellData -> {
            LinkElement cellValue = cellData.getValue();
            BooleanProperty property = cellValue.customProperty();
            property.addListener((observable, oldValue, newValue) -> {
                cellValue.setCustom(newValue);
                linkTable.refresh();
            });
            return property;
        });

        linkTableColumnTable1.setCellValueFactory(column -> new ReadOnlyObjectWrapper<>(column.getValue()));
        linkTableColumnTable1.setCellFactory(param -> new LinkTableCell(mainController, "table1"));

        linkTableColumnTable2.setCellValueFactory(column -> new ReadOnlyObjectWrapper<>(column.getValue()));
        linkTableColumnTable2.setCellFactory(param -> new LinkTableCell(mainController, "table2"));

        linkTableJoinCondition.setCellValueFactory(features -> new ReadOnlyObjectWrapper<>(features.getValue()));
        linkTableJoinCondition.setCellFactory(column -> new JoinConditionCell());
    }

    public TableView<LinkElement> loadLinks(PlainSelect pSelect) {
        TableView<LinkElement> linkTable = new TableView<>();
        List<Join> joins = pSelect.getJoins();
        if (joins == null) {
            return linkTable;
        }

        Table fromItem = (Table) pSelect.getFromItem();

        for (Join join : joins) {
            FromItem rightItem = join.getRightItem();
            if (rightItem instanceof Table) {
                addLinkRow(linkTable, fromItem, join);
            } else if (rightItem instanceof SubSelect) {
//                SubSelect sSelect = (SubSelect) rightItem;
//                rightItemName = sSelect.getAlias().getName();
//                TableRow tableRow = new TableRow(rightItemName);
//                tableRow.setNested(true);
//                tableRow.setRoot(true);
//                String queryText = sSelect.toString().replace(sSelect.getAlias().toString(), "");
//                queryText = queryText.substring(1, queryText.length() - 1);
//                tableRow.setQuery(queryText);
//                TreeItem<TableRow> tableRowTreeItem = new TreeItem<>(tableRow);
//                tablesView.getRoot().getChildren().add(tableRowTreeItem);
//
//                PlainSelect plainSelect = (PlainSelect) sSelect.getSelectBody();
//                plainSelect.getSelectItems().forEach((sItem) -> {
//                    TableRow nestedItem = new TableRow(sItem.toString());
//                    TreeItem<TableRow> nestedRow = new TreeItem<>(nestedItem);
//                    tableRowTreeItem.getChildren().add(nestedRow);
//                });

            }
        }
        return linkTable;
    }

    private void addLinkRow(TableView<LinkElement> linkTable, Table table, Join join) {
        if (join.getOnExpression() == null) {
            return;
        }

        Expression onExpression = join.getOnExpression();
        if (onExpression instanceof AndExpression) {
            AndExpression expression = (AndExpression) onExpression;
            while (true) {
                Expression rightExpression = expression.getRightExpression();
                LinkElement linkElement = new LinkElement(
                        mainController, table.getName(), join.getRightItem().toString(),
                        isLeft(join), isRight(join), isCustom(rightExpression)
                );

                setSimpleCondition(linkElement, rightExpression);
                linkTable.getItems().add(linkElement);
                if (!(expression.getLeftExpression() instanceof AndExpression)) {
                    Expression lExpression = expression.getLeftExpression();
                    LinkElement linkElement2 = new LinkElement(
                            mainController, table.getName(), join.getRightItem().toString(),
                            isLeft(join), isRight(join), isCustom(lExpression)
                    );
                    setSimpleCondition(linkElement2, lExpression);
                    linkTable.getItems().add(linkElement2);
                    break;
                }
                expression = (AndExpression) expression.getLeftExpression();
            }
        } else {
            Expression expression = join.getOnExpression();
            LinkElement linkElement = new LinkElement(
                    mainController, table.getName(), join.getRightItem().toString(),
                    isLeft(join), isRight(join), isCustom(expression)
            );
            setSimpleCondition(linkElement, expression);
            linkTable.getItems().add(linkElement);
        }
    }

    private static void setSimpleCondition(LinkElement linkElement, Expression expression) {
        String cond = expression.toString();
        if (expression instanceof ComparisonOperator) {
            ComparisonOperator expr = (ComparisonOperator) expression;
            Column leftColumn = (Column) expr.getLeftExpression();
            Column rightColumn = (Column) expr.getRightExpression();
            cond = leftColumn.getColumnName() + expr.getStringExpression() + rightColumn.getColumnName();

            linkElement.setField1(leftColumn.getColumnName());
            linkElement.setField2(rightColumn.getColumnName());
            linkElement.setExpression(expr.getStringExpression());
        }

        linkElement.setCondition(cond);
    }

    private static boolean isLeft(Join join) {
        if (join.isInner()) {
            return false;
        } else {
            return join.isFull() || join.isLeft();
        }
    }

    private static boolean isRight(Join join) {
        if (join.isInner()) {
            return false;
        } else {
            return join.isFull() || join.isRight();
        }
    }

    private static boolean isCustom(Expression expression) {
        return !(expression instanceof ComparisonOperator);
    }
}
