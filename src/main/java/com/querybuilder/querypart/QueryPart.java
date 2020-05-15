package com.querybuilder.querypart;

import net.sf.jsqlparser.statement.select.PlainSelect;

public abstract class QueryPart {
    abstract void load(PlainSelect select);

    abstract void save();
}
