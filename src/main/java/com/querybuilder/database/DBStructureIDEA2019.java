package com.querybuilder.database;

import com.intellij.database.console.JdbcConsole;
import com.intellij.database.dataSource.DataSourceSchemaMapping;
import com.intellij.database.dataSource.DatabaseDriver;
import com.intellij.database.dataSource.LocalDataSource;
import com.intellij.database.model.DasObject;
import com.intellij.database.model.ObjectKind;
import com.intellij.database.util.DasUtil;
import com.intellij.database.util.ObjectPath;
import com.intellij.database.util.ObjectPaths;
import com.intellij.database.util.TreePattern;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.containers.JBTreeTraverser;
import com.querybuilder.domain.TableRow;
import javafx.scene.control.TreeItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.querybuilder.utils.Constants.DATABASE_TABLE_ROOT;

public class DBStructureIDEA2019 implements DBStructure {
    private Map<String, List<String>> dbElements;

    @Override
    public TreeItem<TableRow> getDBStructure(JdbcConsole console) {
        dbElements = new HashMap<>();

        TableRow tablesRoot = new TableRow(DATABASE_TABLE_ROOT);
        tablesRoot.setRoot(true);
        TreeItem<TableRow> root = new TreeItem<>(tablesRoot);
        root.setExpanded(true);

        LocalDataSource dataSource = console.getDataSource();
        DatabaseDriver databaseDriver = dataSource.getDatabaseDriver();
        if (databaseDriver == null) {
            throw new IllegalStateException("Database driver not set");
        }

        String sqlDialect = databaseDriver.getSqlDialect();
        if (!sqlDialect.equals("PostgreSQL")) {
            throw new IllegalStateException("Not supported yet");
        }

        TreePattern scope = console.getDataSource().getIntrospectionScope();
        JBTreeTraverser<DasObject> databases = dataSource.getModel().traverser().expand(
                DasUtil.byKind(ObjectKind.DATABASE)
        );
        for (DasObject database : databases) {
            ObjectPath path = ObjectPaths.of(database);
            JBIterable<DasObject> schemas = dataSource.getModel()
                    .traverser()
                    .expandAndSkip(
                            x -> x.getKind() == ObjectKind.DATABASE && x.getName().equals(path.getName()))
                    .filter(DasUtil.byKind(ObjectKind.SCHEMA)).traverse();

            for (DasObject schema : schemas) {
                if (!DataSourceSchemaMapping.isIntrospected(scope, schema)) {
                    continue;
                }
                JBIterable<? extends DasObject> dasChildren1 = schema.getDasChildren(ObjectKind.TABLE);
                for (DasObject object : dasChildren1) {
                    addToStructure(object, root);
                }
                break;
            }
        }

        return root;
    }

    private void addToStructure(DasObject table, TreeItem<TableRow> root) {
        TableRow parentNode = new TableRow(table.getName());
        parentNode.setRoot(true);
        TreeItem<TableRow> stringTreeItem = new TreeItem<>(parentNode);
        root.getChildren().add(stringTreeItem);
        List<String> tableElements = new ArrayList<>();
        table.getDasChildren(ObjectKind.COLUMN).forEach(column -> {
            tableElements.add(column.getName());
            stringTreeItem.getChildren().add(new TreeItem<>(new TableRow(column.getName())));
        });
        dbElements.put(table.getName(), tableElements);
    }

    @Override
    public Map<String, List<String>> getDbElements() {
        if (dbElements == null) {
            System.out.println("DATA SOURCE NOT SET");
        }
        return dbElements;
    }
}
