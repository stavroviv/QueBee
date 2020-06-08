package com.querybuilder.querypart;

import com.querybuilder.controllers.MainController;

public abstract class AbstractQueryPart {
    protected MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    abstract public void initialize();
}
