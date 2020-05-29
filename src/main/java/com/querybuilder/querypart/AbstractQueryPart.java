package com.querybuilder.querypart;

import com.querybuilder.controllers.MainController;
import net.sf.jsqlparser.statement.select.PlainSelect;

public abstract class AbstractQueryPart {
    protected MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    abstract public void initialize();

    abstract public void load(PlainSelect pSelect);

    abstract public void save(PlainSelect pSelect);
}
