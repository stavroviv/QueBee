package com.querybuilder.home.lab;

import com.intellij.database.model.DasObject;
import com.intellij.database.model.ObjectKind;
import com.intellij.database.psi.DbDataSource;
import com.intellij.database.psi.DbDataSourceImpl;
import com.intellij.database.util.DbUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.util.containers.JBIterable;
import javafx.scene.control.TreeItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DBStructureImpl implements DBStructure {
    private Map<String, List<String>> dbElements;

    @Override
    public TreeItem<TableRow> getDBStructure() {
        dbElements = new HashMap<>();
        Project p = ProjectManager.getInstance().getOpenProjects()[0];
        JBIterable<DbDataSource> dataSources = DbUtil.getDataSources(p);
        DbDataSourceImpl dbDataSource = (DbDataSourceImpl) dataSources.get(0);

        TableRow tablesRoot = new TableRow("Tables");
        tablesRoot.setRoot(true);
        TreeItem<TableRow> root = new TreeItem<>(tablesRoot);
        root.setExpanded(true);

        JBIterable<? extends DasObject> modelRoots = dataSources.get(0).getModel().getModelRoots();
        if (dbDataSource.getSqlDialect().getID().equals("PostgreSQL")) {
            modelRoots
                    .find(x -> x.getKind().equals(ObjectKind.DATABASE))
                    .getDasChildren(ObjectKind.SCHEMA)
                    .find(x -> x.getKind().equals(ObjectKind.SCHEMA))
                    .getDasChildren(ObjectKind.TABLE).forEach(table -> addToStructure(table, root));
        } else if (dbDataSource.getSqlDialect().getID().equals("MySQL")) {
            modelRoots
                    .find(x -> x.getKind().equals(ObjectKind.SCHEMA))
                    .getDasChildren(ObjectKind.TABLE).forEach(table -> addToStructure(table, root));
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
        return dbElements;
    }
}
