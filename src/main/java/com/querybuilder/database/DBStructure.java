package com.querybuilder.database;

import com.intellij.database.console.JdbcConsole;
import com.querybuilder.domain.DBTables;

public interface DBStructure {

    DBTables getDBStructure(JdbcConsole console);

}
