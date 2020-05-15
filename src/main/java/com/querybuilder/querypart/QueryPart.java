package com.querybuilder.querypart;

import com.querybuilder.controllers.MainController;
import net.sf.jsqlparser.statement.select.PlainSelect;

public interface QueryPart {
    void load(MainController controller, PlainSelect select) throws Exception;

    void save(MainController controller, PlainSelect select) throws Exception;
}
