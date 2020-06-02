package com.querybuilder.database;

import com.intellij.database.console.JdbcConsole;
import com.querybuilder.domain.TableRow;
import javafx.scene.control.TreeItem;

import java.util.List;
import java.util.Map;

public interface DBStructure {

    TreeItem<TableRow> getDBStructure(JdbcConsole console);

    Map<String, List<String>> getDbElements();
}
