package com.querybuilder.database;

import com.intellij.database.console.JdbcConsole;
import com.intellij.database.dataSource.DataSourceSchemaMapping;
import com.intellij.database.dataSource.LocalDataSource;
import com.intellij.database.model.DasObject;
import com.intellij.database.model.ObjectKind;
import com.intellij.database.util.*;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.containers.JBTreeTraverser;
import com.querybuilder.domain.TableRow;
import javafx.scene.control.TreeItem;

import java.util.*;

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

        ObjectPath currentNamespace = console.getCurrentNamespace();
        JBIterable<? extends DasObject> modelRoots = dataSource.getModel().getModelRoots();
        String sqlDialect = dataSource.getDatabaseDriver().getSqlDialect();

        TreePattern scope = console.getDataSource().getIntrospectionScope();

        if (!sqlDialect.equals("PostgreSQL")) {
            throw new IllegalStateException("Not supported yet");
        }

        ObjectPath cur = SearchPath.getCurrent(console.getSearchPath());

        dataSource.getIntrospectionScope();
        Iterator var9 = ((JBTreeTraverser) dataSource.getModel().traverser().expand(DasUtil.byKind(ObjectKind.DATABASE))).iterator();
        while (var9.hasNext()) {
            DasObject namespace = (DasObject) var9.next();
            ObjectPath path = ObjectPaths.of(namespace);

            //List<ObjectPath> searchPath = new ArrayList(SearchPath.getElements(console.getSearchPath()));

            Iterator var10000 = ((JBTreeTraverser) dataSource.getModel()
                    .traverser()
                    .expandAndSkip(
                            x -> x.getKind() == ObjectKind.DATABASE && x.getName().equals(path.getName()))
            ).filter(DasUtil.byKind(ObjectKind.SCHEMA)).traverse().iterator();

            while (var10000.hasNext()) {
                DasObject schema = (DasObject) var10000.next();
                boolean introspected = DataSourceSchemaMapping.isIntrospected(scope, schema);
                if (introspected) {
                    JBIterable<? extends DasObject> dasChildren1 = schema.getDasChildren(ObjectKind.TABLE);
                    for (DasObject object : dasChildren1) {
                        addToStructure(object, root);
                    }
                    break;
                }

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
