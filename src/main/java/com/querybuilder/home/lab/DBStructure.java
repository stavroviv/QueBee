package com.querybuilder.home.lab;

import com.intellij.database.dataSource.LocalDataSource;
import javafx.scene.control.TreeItem;

import java.util.List;
import java.util.Map;

public interface DBStructure {
    TreeItem<TableRow> getDBStructure(LocalDataSource dataSource);
    Map<String, List<String>> getDbElements();
//    void setDataSource(LocalDataSource dataSource);
}
