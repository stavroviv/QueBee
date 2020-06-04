package com.querybuilder.querypart;

import com.querybuilder.controllers.MainController;
import com.querybuilder.domain.DBTables;
import com.querybuilder.domain.SelectedFieldsTree;
import com.querybuilder.domain.TableRow;
import com.querybuilder.eventbus.CustomEvent;
import com.querybuilder.eventbus.CustomEventBus;
import com.querybuilder.eventbus.Subscriber;
import com.querybuilder.utils.CustomCell;
import com.querybuilder.utils.Utils;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.engio.mbassy.listener.Handler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.parser.ParseException;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.querybuilder.controllers.SelectedFieldController.FIELD_FORM_CLOSED_EVENT;
import static com.querybuilder.domain.ConditionCell.REFRESH_SELECTED_TREE;
import static com.querybuilder.utils.Constants.*;
import static com.querybuilder.utils.Utils.*;

@Data
@EqualsAndHashCode(callSuper = false)
public class FromTables extends AbstractQueryPart implements Subscriber {
    @FXML
    private Button addInnerQuery;
    @FXML
    private TreeTableView<TableRow> tablesView;
    @FXML
    private TreeTableColumn<TableRow, TableRow> tablesViewColumn;
    @FXML
    private TableView<TableRow> fieldTable;
    @FXML
    private TreeTableView<TableRow> databaseTableView;
    @FXML
    private TreeTableColumn<TableRow, TableRow> databaseTableColumn;
    @FXML
    private TableColumn<TableRow, String> fieldColumn;

    @FXML
    @Override
    public void initialize() {
        tablesView.setRoot(new TreeItem<>());

        setCellsFactories();
        setListeners();
        setCellFactory(databaseTableColumn);

        fieldTable.setOnMousePressed(e -> {
            if (doubleClick(e)) {
                editField();
            }
        });
        setStringColumnFactory(fieldColumn);
        CustomEventBus.register(this);
    }

    public void loadDbStructureToTree(DBTables dbStructure) {
        databaseTableView.setRoot(new TreeItem<>());
        databaseTableView.getRoot().getChildren().add(dbStructure.getRoot());
    }

    public void editField() {
        TableRow selectedItem = fieldTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("selectedFieldsTree", mainController.getSelectedGroupFieldsTree());
        data.put("selectedItem", selectedItem);
        data.put("currentRow", fieldTable.getSelectionModel().getSelectedIndex());
        Utils.openForm("/forms/selected-field.fxml", "Custom expression", data);
    }

    private void setListeners() {
        databaseTableView.setOnMousePressed(e -> {
            if (!doubleClick(e)) {
                return;
            }
            TreeItem<TableRow> selectedItem = databaseTableView.getSelectionModel().getSelectedItem();
            String parent = selectedItem.getParent().getValue().getName();
            String field = selectedItem.getValue().getName();
            if (DATABASE_TABLE_ROOT.equals(parent) || CTE_ROOT.equals(parent)) {
                addTablesRow(mainController, field);
            } else {
                addTablesRow(mainController, parent);
                addFieldRow(parent + "." + field);
            }
        });

        tablesView.getRoot().getChildren().addListener((ListChangeListener<TreeItem<TableRow>>) change -> {
            while (change.next()) {
                List<SelectedFieldsTree> selectedFieldTrees = new ArrayList<>();
                selectedFieldTrees.add(mainController.getSelectedGroupFieldsTree());
                selectedFieldTrees.add(mainController.getSelectedConditionsTreeTable());
                selectedFieldTrees.add(mainController.getSelectedOrderFieldsTree());
                applyChange(selectedFieldTrees, selectedFieldsTree -> selectedFieldsTree.applyChanges(change));

                CustomEvent customEvent = new CustomEvent();
                customEvent.setName(REFRESH_SELECTED_TREE);
                customEvent.setChange(change);
                CustomEventBus.post(customEvent);
            }
        });
        tablesView.setOnMousePressed(e -> {
            if (!doubleClick(e)) {
                return;
            }
            TreeItem<TableRow> selectedItem = tablesView.getSelectionModel().getSelectedItem();
            String parent = selectedItem.getParent().getValue().getName();
            String field = selectedItem.getValue().getName();
            if (!TABLES_ROOT.equals(parent)) {
                addFieldRow(parent + "." + field);
            }
        });

        fieldTable.getItems().addListener((ListChangeListener<TableRow>) change -> {
            while (change.next()) {
                List<SelectedFieldsTree> selectedFieldTrees = new ArrayList<>();
                selectedFieldTrees.add(mainController.getSelectedGroupFieldsTree());
                selectedFieldTrees.add(mainController.getSelectedOrderFieldsTree());
                applyChange(selectedFieldTrees, selectedFieldsTree -> selectedFieldsTree.applyChangesString(change));
            }
        });
    }

    private static void applyChange(List<SelectedFieldsTree> fieldsTree, Consumer<SelectedFieldsTree> consumer) {
        fieldsTree.stream().filter(Objects::nonNull).forEach(consumer);
    }

    private void setCellsFactories() {
        setCellFactory(tablesViewColumn);
        tablesViewColumn.setCellFactory(ttc -> new CustomCell() {
            @Override
            protected void updateItem(TableRow item, boolean empty) {
                super.updateItem(item, empty);
                setItem(this, item, empty);
                setContextMenu(tableViewGetContextMenu(item, empty));
            }
        });
    }

    private ContextMenu tableViewGetContextMenu(TableRow item, boolean empty) {
        MenuItem addContext = new MenuItem("Add");
        MenuItem changeContext = new MenuItem("Change");
        MenuItem deleteContext = new MenuItem("Delete");
        MenuItem renameContext = new MenuItem("Rename");

        addContext.setOnAction((ActionEvent event) -> {
//            System.out.println("addContext");
//            Object item = tablesView.getSelectionModel().getSelectedItem();
//            System.out.println("Selected item: " + item);
        });
        deleteContext.setOnAction((ActionEvent event) -> deleteTableFromSelected());
        renameContext.setOnAction((ActionEvent event) -> {
//            System.out.println("renameContext");
//            Object item = tablesView.getSelectionModel().getSelectedItem();
//            System.out.println("Selected item: " + item);
        });
        changeContext.setOnAction((ActionEvent event) -> {
//            System.out.println("changeContext");
//            TableRow item = tablesView.getSelectionModel().getSelectedItem().getValue();
            openNestedQuery(item.getQuery(), item);
        });

        ContextMenu menu = new ContextMenu();
        menu.getItems().add(addContext);
        if (!empty && item.isRoot()) {
            if (item.isNested()) {
                menu.getItems().add(changeContext);
            }
            menu.getItems().add(deleteContext);
            menu.getItems().add(renameContext);
        }
        return menu;
    }

    private void addTablesRow(MainController controller, String parent) {
        ObservableList<TreeItem<TableRow>> children = tablesView.getRoot().getChildren();
        if (children.stream().noneMatch(x -> x.getValue().getName().equals(parent))) {
            tablesView.getRoot().getChildren().add(
                    getTableItemWithFields(controller, parent)
            );
        }
        controller.refreshLinkTable();
    }

    private static TreeItem<TableRow> getTableItemWithFields(MainController controller, String tableName) {
        TableRow tableRoot = new TableRow(tableName);
        tableRoot.setRoot(true);

        AtomicReference<Boolean> cte = new AtomicReference<>(false);
        ObservableList<String> columns = getColumns(controller, tableName, cte);
        TreeItem<TableRow> treeItem = new TreeItem<>(tableRoot);
        columns.forEach(col -> {
            TableRow tableRow = new TableRow(col);
            TreeItem<TableRow> tableRowTreeItem = new TreeItem<>(tableRow);
            treeItem.getChildren().add(tableRowTreeItem);
        });
        treeItem.getValue().setCte(cte.get());

        return treeItem;
    }

    @Override
    public void load(PlainSelect pSelect) {
        loadFromTables(pSelect);
        loadSelectedItems(pSelect);
        TreeHelpers.load(mainController);
    }

    private void loadFromTables(PlainSelect pSelect) {
        FromItem fromItem = pSelect.getFromItem();
        Table table = null;
        if (fromItem instanceof Table) {
            table = (Table) fromItem;
            tablesView.getRoot().getChildren().add(getTableItemWithFields(mainController, table.getName()));
        }
        List<Join> joins = pSelect.getJoins();
        if (joins == null || table == null) {
            return;
        }

        for (Join join : joins) {
            FromItem rightItem = join.getRightItem();
            String rightItemName;
            if (rightItem instanceof Table) {
                rightItemName = rightItem.toString();
                tablesView.getRoot().getChildren().add(getTableItemWithFields(mainController, rightItemName));
            } else if (rightItem instanceof SubSelect) {
                SubSelect sSelect = (SubSelect) rightItem;
                rightItemName = sSelect.getAlias().getName();
                TableRow tableRow = new TableRow(rightItemName);
                tableRow.setNested(true);
                tableRow.setRoot(true);
                String queryText = sSelect.toString().replace(sSelect.getAlias().toString(), "");
                queryText = queryText.substring(1, queryText.length() - 1);
                tableRow.setQuery(queryText);
                TreeItem<TableRow> tableRowTreeItem = new TreeItem<>(tableRow);
                tablesView.getRoot().getChildren().add(tableRowTreeItem);

                PlainSelect plainSelect = (PlainSelect) sSelect.getSelectBody();
                plainSelect.getSelectItems().forEach((sItem) -> {
                    TableRow nestedItem = new TableRow(sItem.toString());
                    TreeItem<TableRow> nestedRow = new TreeItem<>(nestedItem);
                    tableRowTreeItem.getChildren().add(nestedRow);
                });
            }
        }
    }

    public void loadSelectedItems(PlainSelect pSelect) {
        if (pSelect.getSelectItems() == null) {
            return;
        }

        for (SelectItem selectField : pSelect.getSelectItems()) {
            if (selectField instanceof SelectExpressionItem) {

                // GROUPING
                SelectExpressionItem item = (SelectExpressionItem) selectField;
                Expression expression = item.getExpression();

                if (expression instanceof Function
                        && ((Function) item.getExpression()).getParameters().getExpressions().size() == 1) {
                    Function function = (Function) item.getExpression();
                    String columnName = function.getParameters().getExpressions().get(0).toString();
                    TableRow newField = new TableRow(columnName);
                    mainController.getGroupingController().loadAggregate(newField, function);
                    fieldTable.getItems().add(newField);

                } else if (expression instanceof Column) {
                    Column column = (Column) expression;
                    fieldTable.getItems().add(new TableRow(getNameFromColumn(column)));
                } else {
                    fieldTable.getItems().add(new TableRow(selectField.toString()));
                }

            } else {
                fieldTable.getItems().add(new TableRow(selectField.toString()));
            }
        }
    }

    @Override
    public void save(PlainSelect selectBody) {
        saveFromTables(selectBody);
        saveSelectedFields(selectBody);
    }

    public void saveFromTables(PlainSelect selectBody) {
        if (!mainController.getLinksController().getLinkTable().getItems().isEmpty()) {
            return;
        }
        List<Join> joins = new ArrayList<>();
        tablesView.getRoot().getChildren().forEach(x -> {
            String tableName = x.getValue().getName();
            if (selectBody.getFromItem() == null) {
                selectBody.setFromItem(new Table(tableName));
            } else {
                Join join = new Join();
                join.setRightItem(new Table(tableName));
                join.setSimple(true);
                joins.add(join);
            }
        });
        selectBody.setJoins(joins);
    }

    public void saveSelectedFields(PlainSelect selectBody) {
        List<SelectItem> items = new ArrayList<>();
        fieldTable.getItems().forEach(item -> {
            if (mainController.getGroupingController().containAggregate(item)) {
                return;
            }

            String name = item.getName();
            Expression expression = null;
            try {
                expression = new CCJSqlParser(name).Expression();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            SelectExpressionItem sItem = new SelectExpressionItem();
            sItem.setExpression(expression);
            items.add(sItem);
        });
        selectBody.setSelectItems(items);
    }


    @FXML
    public void addInnerQueryOnClick() {
        openNestedQuery("", null);
    }

    public void openNestedQuery(String text, TableRow item) {
//        QueryBuilder qb = new QueryBuilder(text, false, this.queryBuilder.getDataSource());
////        qb.setDataSource();,
//        qb.setParentController(this);
//        qb.setItem(item);
////        qb.setParentController(this);
    }

    @Handler
    public void selectedFieldFormClosedHandler(CustomEvent event) {
        if (!FIELD_FORM_CLOSED_EVENT.equals(event.getName())) {
            return;
        }
        if (event.getCurrentRow() == null) {
            addFieldRow(event.getData());
        } else {
            TableRow tableRow = fieldTable.getItems().get(event.getCurrentRow());
            tableRow.setName(event.getData());
            fieldTable.getItems().set(event.getCurrentRow(), tableRow);
        }
    }

    public void addFieldRow(String name) {
        TableRow newField = new TableRow(name);
        fieldTable.getItems().add(newField);
        mainController.getUnionAliasesController().addAlias(newField);
    }

    @FXML
    public void deleteTableFromSelected() {
        int selectedItem = tablesView.getSelectionModel().getSelectedIndex();
        tablesView.getRoot().getChildren().remove(selectedItem);
    }

    @FXML
    public void addFieldRowAction() {
        Map<String, Object> data = new HashMap<>();
        data.put("selectedFieldsTree", mainController.getSelectedGroupFieldsTree());
        Utils.openForm("/forms/selected-field.fxml", "Custom expression", data);
    }

    @FXML
    public void deleteFieldRow() {
        int selectedItem = fieldTable.getSelectionModel().getSelectedIndex();
        TableRow tableRow = fieldTable.getItems().get(selectedItem);
        fieldTable.getItems().remove(tableRow);
        mainController.getUnionAliasesController().deleteAlias(tableRow);
    }

    @FXML
    private void editFieldClick() {
        editField();
    }

    @Override
    public void initData(Map<String, Object> data) {

    }
}
